package co.colaborativa.UserApiStorageCustom;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.ImportSynchronization;
import org.keycloak.storage.user.SynchronizationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyFileUserStorageProviderFactory
		implements UserStorageProviderFactory<PropertyFileUserStorageProvider>, ImportSynchronization {

	public static final String PROVIDER_NAME = "userapi-quarkus";
	private static final Logger logger = LoggerFactory.getLogger(PropertyFileUserStorageProviderFactory.class);
	private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();
	protected Properties properties = new Properties();
	protected Config.Scope config;

	static {
		ProviderConfigProperty property;
		property = new ProviderConfigProperty();
		property.setName("cookie.max.age");
		property.setLabel("Cookie Max Age");
		property.setType(ProviderConfigProperty.STRING_TYPE);
		property.setHelpText("Max age in seconds of the SECRET_QUESTION_COOKIE.");
		configProperties.add(property);
	}

	@Override
	public void close() {
		logger.info("close()");
	}

	@Override
	public UserStorageProvider create(KeycloakSession session) {
		logger.info("create(KeycloakSession session)");

		return new PropertyFileUserStorageProvider(session);
	}

	@Override
	public PropertyFileUserStorageProvider create(KeycloakSession session, ComponentModel model) {
		logger.info("create(KeycloakSession session, ComponentModel model)");

		return new PropertyFileUserStorageProvider(session, model, properties);
	}

	@Override
	public List<ProviderConfigProperty> getCommonProviderConfigProperties() {
		logger.info("getCommonProviderConfigProperties()");

		return Collections.emptyList();
	}

	@Override
	public List<ProviderConfigProperty> getConfigProperties() {
		logger.info("getConfigProperties()");

		return configProperties;
	}

	@Override
	public String getHelpText() {
		return "getHelpText";
	}

	@Override
	public String getId() {
		return PROVIDER_NAME;
	}

	@Override
	public Map<String, Object> getTypeMetadata() {
		return Collections.emptyMap();
	}

	@Override
	public void init(Config.Scope config) {
		logger.info("init(Config.Scope config)");

		this.config = config;
	}

	@Override
	public void onCreate(KeycloakSession session, RealmModel realm, ComponentModel model) {
		logger.info("onCreate(KeycloakSession session, RealmModel realm, ComponentModel model)");
	}

	@Override
	public void onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel) {
		logger.info(
				"onUpdate(KeycloakSession session, RealmModel realm, ComponentModel oldModel, ComponentModel newModel)");
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		logger.info("postInit(KeycloakSessionFactory factory)");
	}

	@Override
	public void preRemove(KeycloakSession session, RealmModel realm, ComponentModel model) {
		logger.info("preRemove(KeycloakSession session, RealmModel realm, ComponentModel model)");
	}

	@Override
	public SynchronizationResult sync(KeycloakSessionFactory sessionFactory, String realmId,
			UserStorageProviderModel model) {
		logger.info("sync()");
		
		return SynchronizationResult.ignored();
	}

	@Override
	public SynchronizationResult syncSince(Date lastSync, KeycloakSessionFactory sessionFactory, String realmId,
			UserStorageProviderModel model) {
		logger.info("syncSince()");
		
		return SynchronizationResult.ignored();
	}

	@Override
	public void validateConfiguration(KeycloakSession session, RealmModel realm, ComponentModel config)
			throws ComponentValidationException {
	}
}
