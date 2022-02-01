package org.opennms.core.schema;

import org.opennms.core.schema.migrator.ClassLoaderBasedLiquibaseExecutor;
import org.opennms.core.schema.migrator.ClassLoaderBasedMigratorResourceProvider;
import org.opennms.core.schema.migrator.Migrator;
import org.ops4j.pax.jdbc.hook.PreHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

public class OpenNMSDatabasePrehook implements PreHook {

    private Logger log = LoggerFactory.getLogger(OpenNMSDatabasePrehook.class);

    private boolean enabled = true;
    private DataSource adminDatasource;
    private Properties opennmsDatasourceProperties;
    private Properties opennmsAdminDatasourceProperties;

    private ClassLoader liquibaseResourceClassLoader = getClass().getClassLoader();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DataSource getAdminDatasource() {
        return adminDatasource;
    }

    public void setAdminDatasource(DataSource adminDatasource) {
        this.adminDatasource = adminDatasource;
    }

    public Properties getOpennmsDatasourceProperties() {
        return opennmsDatasourceProperties;
    }

    public void setOpennmsDatasourceProperties(Properties opennmsDatasourceProperties) {
        this.opennmsDatasourceProperties = opennmsDatasourceProperties;
    }

    public Properties getOpennmsAdminDatasourceProperties() {
        return opennmsAdminDatasourceProperties;
    }

    public void setOpennmsAdminDatasourceProperties(Properties opennmsAdminDatasourceProperties) {
        this.opennmsAdminDatasourceProperties = opennmsAdminDatasourceProperties;
    }

    public ClassLoader getLiquibaseResourceClassLoader() {
        return liquibaseResourceClassLoader;
    }

    public void setLiquibaseResourceClassLoader(ClassLoader liquibaseResourceClassLoader) {
        this.liquibaseResourceClassLoader = liquibaseResourceClassLoader;
    }

//========================================
//
//----------------------------------------

    // public void onAdminDatasourceBind(ServiceReference serviceReference) {
    //     serviceReference.getProperty();
    // }



//========================================
// PreHook Operations
//----------------------------------------

    @Override
    public void prepare(DataSource dataSource) throws SQLException {
        if (enabled) {
            // MigratorAdminInitialize migratorAdminInitialize = new MigratorAdminInitialize();
            //
            // migratorAdminInitialize.setAdminDataSource(adminDatasource);
            //
            // migratorAdminInitialize.setAdminUser(opennmsAdminDatasourceProperties.getProperty("user"));
            // migratorAdminInitialize.setAdminPassword(opennmsAdminDatasourceProperties.getProperty("password"));
            // migratorAdminInitialize.setDatabaseName(opennmsDatasourceProperties.getProperty("databaseName"));
            // migratorAdminInitialize.setDatabaseUser(opennmsDatasourceProperties.getProperty("user"));
            // migratorAdminInitialize.setDatabasePassword(opennmsDatasourceProperties.getProperty("password"));
            // migratorAdminInitialize.setValidateDatabaseVersion(true); // TBD999: configurable property

            this.log.info("INITIALIZE DATABASE");

            try {
                initializeDb();
                migrateDb(dataSource);
            } catch (SQLException sqlExc) {
                throw sqlExc;
            } catch (Exception exc) {
                throw new RuntimeException("Failed to initialize database", exc);
            }
        } else {
            log.info("DATABASE INITIALIZATION DISABLED");
        }
    }

//========================================
// Internals
//----------------------------------------

    private void initializeDb() throws Exception {
        MigratorAdminInitialize migratorAdminInitialize = new MigratorAdminInitialize();

        migratorAdminInitialize.setAdminDataSource(adminDatasource);
        migratorAdminInitialize.setAdminUser(opennmsAdminDatasourceProperties.getProperty("user"));
        migratorAdminInitialize.setAdminPassword(opennmsAdminDatasourceProperties.getProperty("password"));

        migratorAdminInitialize.setDatabaseName(opennmsDatasourceProperties.getProperty("databaseName"));
        migratorAdminInitialize.setDatabaseUser(opennmsDatasourceProperties.getProperty("user"));
        migratorAdminInitialize.setDatabasePassword(opennmsDatasourceProperties.getProperty("password"));
        migratorAdminInitialize.setValidateDatabaseVersion(true); // TBD999: configurable property

        migratorAdminInitialize.initializeDatabase(true, false);
    }

    private void migrateDb(DataSource dataSource) throws Exception {
        ClassLoaderBasedMigratorResourceProvider resourceProvider =
                new ClassLoaderBasedMigratorResourceProvider(liquibaseResourceClassLoader);
        ClassLoaderBasedLiquibaseExecutor liquibaseExecutor =
                new ClassLoaderBasedLiquibaseExecutor(liquibaseResourceClassLoader);

        Migrator migrator = new Migrator(resourceProvider, liquibaseExecutor);

        migrator.setAdminDataSource(adminDatasource);
        migrator.setAdminUser(opennmsAdminDatasourceProperties.getProperty("user"));
        migrator.setAdminPassword(opennmsAdminDatasourceProperties.getProperty("password"));

        migrator.setDataSource(dataSource);
        migrator.setDatabaseName(opennmsDatasourceProperties.getProperty("databaseName"));
        migrator.setSchemaName(opennmsDatasourceProperties.getProperty("schema"));
        migrator.setDatabaseUser(opennmsDatasourceProperties.getProperty("user"));
        migrator.setDatabasePassword(opennmsDatasourceProperties.getProperty("password"));

        this.log.info("STARTING MIGRATOR");

        migrator.setupDatabase(true, false, false, true, false);
    }
}
