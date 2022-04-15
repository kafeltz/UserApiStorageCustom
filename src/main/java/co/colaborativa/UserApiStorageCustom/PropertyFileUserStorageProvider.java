package co.colaborativa.UserApiStorageCustom;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.keycloak.common.util.EnvUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.ImportedUserValidation;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

public class PropertyFileUserStorageProvider
		implements UserStorageProvider, UserLookupProvider, CredentialInputValidator, CredentialInputUpdater,
		UserRegistrationProvider, UserQueryProvider, ImportedUserValidation {

	public static final String UNSET_PASSWORD = "#$!-UNSET-PASSWORD";
	private static final Set<String> disableableTypes = new HashSet<>();

	static {
		disableableTypes.add(CredentialModel.PASSWORD);
	}

	protected KeycloakSession session;
	protected Properties properties;
	protected ComponentModel model;

	// map of loaded users in this transaction
	protected Map<String, UserModel> loadedUsers = new HashMap<>();

	public PropertyFileUserStorageProvider(KeycloakSession session) {
		this.session = session;
	}

	public PropertyFileUserStorageProvider(KeycloakSession session, ComponentModel model) {
		this.session = session;
		this.model = model;
	}

	public PropertyFileUserStorageProvider(KeycloakSession session, ComponentModel model, Properties properties) {
		this.session = session;
		this.model = model;
		this.properties = properties;
	}

	@Override
	public UserModel addUser(RealmModel realm, String username) {
		synchronized (properties) {
			properties.setProperty(username, UNSET_PASSWORD);
			save();
		}
		return createAdapter(realm, username);
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

//	protected UserModel createAdapter(RealmModel realm, final String username) {
//		return new AbstractUserAdapter(session, realm, model) {
//			@Override
//			public String getUsername() {
//				return username;
//			}
//		};
//	}

//	protected UserModel createAdapter(RealmModel realm, final String username) {
//		return new AbstractUserAdapterFederatedStorage(session, realm, model) {
//			@Override
//			public String getUsername() {
//				return username;
//			}
//
//			@Override
//			public void setUsername(String username) {
//				String pw = (String) properties.remove(username);
//				if (pw != null) {
//					properties.put(username, pw);
//					save();
//				}
//			}
//		};
//	}

	protected UserModel createAdapter(RealmModel realm, String username) {
		UserModel local = session.userLocalStorage().getUserByUsername(username, realm);

		if (local == null) {
			local = session.userLocalStorage().addUser(realm, username);
			local.setFederationLink(model.getId());
		}

		return new UserModelDelegate(local) {
			@Override
			public void setUsername(String username) {
				String pw = (String) properties.remove(username);
				if (pw != null) {
					properties.put(username, pw);
					save();
				}
				super.setUsername(username);
			}
		};
	}

	@Override
	public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
		if (!credentialType.equals(CredentialModel.PASSWORD))
			return;
		synchronized (properties) {
			properties.setProperty(user.getUsername(), UNSET_PASSWORD);
			save();
		}

	}

	@Override
	public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
		return disableableTypes;
	}

//	@Override
//	public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
//		if (!supportsCredentialType(input.getType()))
//			return false;
//
//		String password = properties.getProperty(user.getUsername());
//		if (password == null)
//			return false;
//		return password.equals(input.getChallengeResponse());
//	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
		return Collections.emptyList();
	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
		return Collections.emptyList();
	}

	@Override
	public UserModel getUserByEmail(String email, RealmModel realm) {
		return null;
	}

	@Override
	public UserModel getUserById(String id, RealmModel realm) {
		StorageId storageId = new StorageId(id);
		String username = storageId.getExternalId();
		return getUserByUsername(username, realm);
	}

//	@Override
//	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
//		if (input.getType().equals(PasswordCredentialModel.TYPE))
//			throw new ReadOnlyException("user is read only for this update");
//
//		return false;
//	}

	@Override
	public UserModel getUserByUsername(String username, RealmModel realm) {
		UserModel adapter = loadedUsers.get(username);
		if (adapter == null) {
			String password = properties.getProperty(username);
			if (password != null) {
				adapter = createAdapter(realm, username);
				loadedUsers.put(username, adapter);
			}
		}
		return adapter;
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm) {
		return getUsers(realm, 0, Integer.MAX_VALUE);
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
		List<UserModel> users = new LinkedList<>();
		int i = 0;
		for (Object obj : properties.keySet()) {
			if (i++ < firstResult)
				continue;
			String username = (String) obj;
			UserModel user = getUserByUsername(username, realm);
			users.add(user);
			if (users.size() >= maxResults)
				break;
		}
		return users;
	}

	@Override
	public int getUsersCount(RealmModel realm) {
		return properties.size();
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		String password = properties.getProperty(user.getUsername());
		return credentialType.equals(PasswordCredentialModel.TYPE) && password != null;
	}

	@Override
	public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
		if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel))
			return false;

		UserCredentialModel cred = (UserCredentialModel) input;
		String password = properties.getProperty(user.getUsername());
		if (password == null || UNSET_PASSWORD.equals(password))
			return false;
		return password.equals(cred.getValue());
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		synchronized (properties) {
			if (properties.remove(user.getUsername()) == null)
				return false;
			save();
			return true;
		}
	}

	public void save() {
		String path = model.getConfig().getFirst("path");
		path = EnvUtil.replace(path);

		try {
			FileOutputStream fos = new FileOutputStream(path);
			properties.store(fos, "");
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult,
			int maxResults) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm) {
		return searchForUser(search, realm, 0, Integer.MAX_VALUE);
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
		List<UserModel> users = new LinkedList<>();
		int i = 0;
		for (Object obj : properties.keySet()) {
			String username = (String) obj;
			if (!username.contains(search))
				continue;
			if (i++ < firstResult)
				continue;
			UserModel user = getUserByUsername(username, realm);
			users.add(user);
			if (users.size() >= maxResults)
				break;
		}
		return users;
	}

	@Override
	public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
		return Collections.emptyList();
	}

	@Override
	public boolean supportsCredentialType(String credentialType) {
		return credentialType.equals(PasswordCredentialModel.TYPE);
	}

	@Override
	public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
		if (!(input instanceof UserCredentialModel))
			return false;
		if (!input.getType().equals(CredentialModel.PASSWORD))
			return false;
		UserCredentialModel cred = (UserCredentialModel) input;
		synchronized (properties) {
			properties.setProperty(user.getUsername(), cred.getValue());
			save();
		}
		return true;
	}

	@Override
	public UserModel validate(RealmModel realm, UserModel user) {
		// TODO Auto-generated method stub
		return null;
	}
}
