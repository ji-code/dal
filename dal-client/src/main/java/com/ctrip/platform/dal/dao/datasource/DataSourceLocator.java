package com.ctrip.platform.dal.dao.datasource;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.sql.DataSource;

import com.ctrip.platform.dal.dao.configure.*;
import com.ctrip.platform.dal.dao.helper.DalElementFactory;
import com.ctrip.platform.dal.dao.log.ILogger;

public class DataSourceLocator {

    private static final ILogger LOGGER = DalElementFactory.DEFAULT.getILogger();

//    private static final Map<String, DataSource> cache = new ConcurrentHashMap<>();
    private static final Map<DataSourceIdentity, DataSource> dataSourceCache = new ConcurrentHashMap<>();

    private DatasourceBackgroundExecutor executor = DalElementFactory.DEFAULT.getDatasourceBackgroundExecutor();

    private DataSourceConfigureProvider provider;

    public DataSourceLocator(DataSourceConfigureProvider provider) {
        this.provider = provider;
    }

    // to be refactored
    public static boolean containsKey(String name) {
        return dataSourceCache.containsKey(new DataSourceName(name));
    }

    /**
     * This is used for initialize datasource for thirdparty framework
     */
    public DataSourceLocator() {
        this(new DefaultDataSourceConfigureProvider());
    }

    /**
     * Get DataSource by real db source name
     *
     * @param name
     * @return DataSource
     * @throws NamingException
     */
    public DataSource getDataSource(String name) throws Exception {
        return getDataSource(new DataSourceName(name));
    }

    public DataSource getDataSource(DataSourceIdentity id) {
        DataSource ds = dataSourceCache.get(id);
        if (ds == null) {
            synchronized (dataSourceCache) {
                ds = dataSourceCache.get(id);
                if (ds == null) {
                    try {
                        ds = createDataSource(id);
                        dataSourceCache.put(id, ds);
                    } catch (Throwable t) {
                        String msg = String.format("error when creating datasource: %s", id.getId());
                        LOGGER.error(msg, t);
                        throw new RuntimeException(msg, t);
                    }
                }
            }
        }
        return ds;
    }

/*    private DataSource createDataSource(String name) throws SQLException {
        IDataSourceConfigure config = provider.getDataSourceConfigure(name);
        if (config == null) {
            throw new SQLException("Can not find connection configure for " + name);
        }

        SingleDataSourceConfigureProvider dataSourceConfigureProvider = new SingleDataSourceConfigureProvider(name, provider);
        ForceSwitchableDataSource ds = new ForceSwitchableDataSource(name, dataSourceConfigureProvider);
        provider.register(name, ds);
        executor.execute(ds);

        return ds;
    }*/

    private DataSource createDataSource(DataSourceIdentity id) throws SQLException {
        DataSourceConfigure config = provider.getDataSourceConfigure(id);
        if (config == null) {
            throw new SQLException(String.format("datasource configure not found for %s", id.getId()));
        }

        SingleDataSourceConfigureProvider dataSourceConfigureProvider = new SingleDataSourceConfigureProvider(id, provider);
        ForceSwitchableDataSource ds = new ForceSwitchableDataSource(id.getId(), dataSourceConfigureProvider);
        provider.register(id.getId(), ds);
        executor.execute(ds);

        return ds;
    }

    // TODO: ???????
    public static Map<String, Integer> getActiveConnectionNumber() {
        Map<String, Integer> map = new HashMap<>();/*
        for (Map.Entry<String, DataSource> entry : cache.entrySet()) {
            DataSource dataSource = entry.getValue();
            if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
                org.apache.tomcat.jdbc.pool.DataSource ds = (org.apache.tomcat.jdbc.pool.DataSource) dataSource;
                map.put(entry.getKey(), ds.getActive());
            }
        }*/
        return map;
    }

}
