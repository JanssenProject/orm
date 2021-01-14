/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.sql.operation.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.querydsl.core.QueryException;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.sql.RelationalPathBase;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.dml.SQLDeleteClause;
import com.querydsl.sql.dml.SQLInsertClause;
import com.querydsl.sql.dml.SQLUpdateClause;

import io.jans.orm.exception.extension.PersistenceExtension;
import io.jans.orm.exception.operation.DeleteException;
import io.jans.orm.exception.operation.DuplicateEntryException;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.orm.exception.operation.PersistenceException;
import io.jans.orm.exception.operation.SearchException;
import io.jans.orm.model.AttributeData;
import io.jans.orm.model.AttributeDataModification;
import io.jans.orm.model.AttributeDataModification.AttributeModificationType;
import io.jans.orm.model.BatchOperation;
import io.jans.orm.model.PagedResult;
import io.jans.orm.model.SearchScope;
import io.jans.orm.operation.auth.PasswordEncryptionHelper;
import io.jans.orm.sql.impl.SqlBatchOperationWraper;
import io.jans.orm.sql.model.ConvertedExpression;
import io.jans.orm.sql.model.SearchReturnDataType;
import io.jans.orm.sql.model.TableMapping;
import io.jans.orm.sql.operation.SqlOperationService;
import io.jans.orm.sql.operation.watch.OperationDurationUtil;
import io.jans.orm.util.ArrayHelper;
import io.jans.orm.util.StringHelper;

/**
 * Base service which performs all supported SQL operations
 *
 * @author Yuriy Movchan Date: 12/22/2020
 */
public class SqlOperationServiceImpl implements SqlOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(SqlOperationServiceImpl.class);

    private Properties props;
    private SqlConnectionProvider connectionProvider;

	private boolean disableAttributeMapping = false;

	private PersistenceExtension persistenceExtension;

	private SQLQueryFactory sqlQueryFactory;

	private String schemaName;

	private Path<String> docAlias = ExpressionUtils.path(String.class, DOC_ALIAS);

    @SuppressWarnings("unused")
    private SqlOperationServiceImpl() {
    }

    public SqlOperationServiceImpl(Properties props, SqlConnectionProvider connectionProvider) {
        this.props = props;
        this.connectionProvider = connectionProvider;
        init();
    }

	private void init() {
		this.sqlQueryFactory = connectionProvider.getSqlQueryFactory();
		this.schemaName = connectionProvider.getSchemaName();
	}

    @Override
    public SqlConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    @Override
    public boolean authenticate(String key, String password, String objectClass) throws SearchException {
        return authenticateImpl(key, password, objectClass);
    }

    private boolean authenticateImpl(String key, String password, String objectClass) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        boolean result = false;
        if (password != null) {
	        try {
		        ResultSet entry = lookup(key, objectClass, USER_PASSWORD);
		        Object userPasswordObj = entry.getObject(USER_PASSWORD);
		
		        String userPassword = null;
		        if (userPasswordObj instanceof String) {
		            userPassword = (String) userPasswordObj;
		        }
		
		        if (userPassword != null) {
		        	if (persistenceExtension == null) {
			        	result = PasswordEncryptionHelper.compareCredentials(password, userPassword);
		        	} else {
		        		result = persistenceExtension.compareHashedPasswords(password, userPassword);
		        	}
		        }
	        } catch (SQLException ex) {
	        	throw new SearchException(String.format("Failed to get '%s' attribute", USER_PASSWORD), ex);
	        }
        }

        Duration duration = OperationDurationUtil.instance().duration(startTime);

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        OperationDurationUtil.instance().logDebug("SQL operation: bind, duration: {}, table: {}, key: {}", duration, tableMapping.getTableName(), key);

        return result;
    }

    @Override
    public boolean addEntry(String key, String objectClass, Collection<AttributeData> attributes) throws DuplicateEntryException, PersistenceException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        boolean result = addEntryImpl(tableMapping, key, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: add, duration: {}, table: {}, key: {}, attributes: {}", duration, tableMapping.getTableName(), key, attributes);
        
        return result;
    }

	private boolean addEntryImpl(TableMapping tableMapping, String key, Collection<AttributeData> attributes) throws PersistenceException {
		try {
			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			SQLInsertClause sqlInsertQuery = this.sqlQueryFactory.insert(tableRelationalPath);

			for (AttributeData attribute : attributes) {
				sqlInsertQuery.columns(Expressions.stringPath(attribute.getName()));
				if (Boolean.TRUE.equals(attribute.getMultiValued())) {
					// TODO: convert to JSON Array
					sqlInsertQuery.values(attribute.getValue());
				} else {
					sqlInsertQuery.values(attribute.getValue());
				}
			}

			long rowInserted = sqlInsertQuery.execute();

			return rowInserted == 1;
        } catch (QueryException ex) {
            throw new PersistenceException("Failed to add entry", ex);
        }
	}

    @Override
    public boolean updateEntry(String key, String objectClass, List<AttributeDataModification> mods) throws UnsupportedOperationException, PersistenceException {
        Instant startTime = OperationDurationUtil.instance().now();
        
        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        boolean result = updateEntryImpl(tableMapping, key, mods);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: modify, duration: {}, table: {}, key: {}, mods: {}", duration, tableMapping.getTableName(), key, mods);

        return result;
    }

	private boolean updateEntryImpl(TableMapping tableMapping, String key, List<AttributeDataModification> mods) throws PersistenceException {
		try {
			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			SQLUpdateClause sqlUpdateQuery = this.sqlQueryFactory.update(tableRelationalPath);

			for (AttributeDataModification attributeMod : mods) {
				AttributeData attribute = attributeMod.getAttribute();
				Path path = Expressions.stringPath(attribute.getName());
				AttributeModificationType type = attributeMod.getModificationType();
                if (AttributeModificationType.ADD == type) {
					if (Boolean.TRUE.equals(attribute.getMultiValued())) {
    					// TODO: convert to JSON Array
    					sqlUpdateQuery.set(path, attribute.getValue());
    				} else {
    					sqlUpdateQuery.set(path, attribute.getValue());
    				}
                } else if (AttributeModificationType.REPLACE == type) {
					if (Boolean.TRUE.equals(attribute.getMultiValued())) {
    					// TODO: convert to JSON Array
    					sqlUpdateQuery.set(path, attribute.getValue());
    				} else {
    					sqlUpdateQuery.set(path, attribute.getValue());
    				}
                } else if (AttributeModificationType.REMOVE == type) {
    				sqlUpdateQuery.setNull(path);
                } else {
                    throw new UnsupportedOperationException("Operation type '" + type + "' is not implemented");
                }
			}

			long rowInserted = sqlUpdateQuery.execute();

			return rowInserted == 1;
        } catch (QueryException ex) {
            throw new PersistenceException("Failed to update entry", ex);
        }
	}

    @Override
    public boolean delete(String key, String objectClass) throws EntryNotFoundException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        boolean result = deleteImpl(tableMapping, key);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: delete, duration: {}, table: {}, key: {}", duration, tableMapping.getTableName(), key);

        return result;
    }

	private boolean deleteImpl(TableMapping tableMapping, String key) throws EntryNotFoundException {
		try {
			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			SQLDeleteClause sqlDeleteQuery = this.sqlQueryFactory.delete(tableRelationalPath);
			
			Predicate exp = ExpressionUtils.eq(Expressions.stringPath(SqlOperationService.DOC_ID), Expressions.constant(key));
			sqlDeleteQuery.where(exp);

			long rowInserted = sqlDeleteQuery.execute();

			return rowInserted == 1;
        } catch (QueryException ex) {
            throw new EntryNotFoundException("Failed to delete entry", ex);
        }
	}

    @Override
    public long delete(String key, String objectClass, ConvertedExpression expression, int count) throws DeleteException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

    	long result = deleteImpl(tableMapping, expression, count);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: delete_search, duration: {}, table: {}, key: {}, expression: {}, count: {}", duration, tableMapping.getTableName(), key, expression, count);

        return result;
    }

    private long deleteImpl(TableMapping tableMapping, ConvertedExpression expression, int count) throws DeleteException {
		try {
			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);
			SQLDeleteClause sqlDeleteQuery = this.sqlQueryFactory.delete(tableRelationalPath);

			Predicate exp = (Predicate) expression.expression();
			sqlDeleteQuery.where(exp);

			long rowDeleted = sqlDeleteQuery.execute();

			return rowDeleted;
        } catch (QueryException ex) {
            throw new DeleteException(String.format("Failed to delete entries. Expression: '%s'", expression.expression()));
        }
	}

    @Override
    public boolean deleteRecursively(String key, String objectClass) throws EntryNotFoundException, SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);
        boolean result = deleteRecursivelyImpl(tableMapping, key);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: delete_tree, duration: {}, table: {}, key: {}", duration, tableMapping.getTableName(), key);

        return result;
    }

	private boolean deleteRecursivelyImpl(TableMapping tableMapping, String key) throws SearchException, EntryNotFoundException {
    	LOG.warn("Removing only base key without sub-tree: " + key);
    	return deleteImpl(tableMapping, key);
	}

    @Override
    public ResultSet lookup(String key, String objectClass, String... attributes) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();
        
    	TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

        ResultSet result = lookupImpl(tableMapping, key, attributes);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: lookup, duration: {}, table: {}, key: {}, attributes: {}", duration, tableMapping.getTableName(), key, attributes);

        return result;
    }

	private ResultSet lookupImpl(TableMapping tableMapping, String key, String... attributes) throws SearchException {
		String queryStr;
		try {
			RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);

			Predicate whereExp = ExpressionUtils.eq(Expressions.stringPath(SqlOperationService.DOC_ID),
					Expressions.constant(key));
			Expression<?> attributesExp = buildSelectAttributes(attributes);

			SQLQuery<?> sqlSelectQuery = sqlQueryFactory.select(attributesExp).from(tableRelationalPath)
					.where(whereExp).limit(1);

			queryStr = sqlSelectQuery.getSQL().getSQL();
		} catch (QueryException ex) {
			throw new SearchException(String.format("Failed to lookup entry by key: '%s'", key), ex);
		}

		try (Connection connection = sqlQueryFactory.getConnection()) {
			PreparedStatement pstmt = connection.prepareStatement(queryStr, Statement.RETURN_GENERATED_KEYS);
			pstmt.setObject(1, key);

			return pstmt.executeQuery();
		} catch (SQLException ex) {
			throw new SearchException(String.format("Failed to execute lookup query '%s'  with key: '%s'", queryStr, key), ex);
		}
	}

	@Override
    public <O> PagedResult<ResultSet> search(String key, String objectClass, ConvertedExpression expression, SearchScope scope, String[] attributes, OrderSpecifier<?>[] orderBy,
                                              SqlBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {
        Instant startTime = OperationDurationUtil.instance().now();

        TableMapping tableMapping = connectionProvider.getTableMappingByKey(key, objectClass);

        PagedResult<ResultSet> result = searchImpl(tableMapping, key, expression, scope, attributes, orderBy, batchOperationWraper,
						returnDataType, start, count, pageSize);

        Duration duration = OperationDurationUtil.instance().duration(startTime);
        OperationDurationUtil.instance().logDebug("SQL operation: search, duration: {}, table: {}, key: {}, expression: {}, scope: {}, attributes: {}, orderBy: {}, batchOperationWraper: {}, returnDataType: {}, start: {}, count: {}, pageSize: {}", duration, tableMapping.getTableName(), key, expression, scope, attributes, orderBy, batchOperationWraper, returnDataType, start, count, pageSize);

        return result;
	}

	private <O> PagedResult<ResultSet> searchImpl(TableMapping tableMapping, String key, ConvertedExpression expression, SearchScope scope, String[] attributes, OrderSpecifier<?>[] orderBy,
            SqlBatchOperationWraper<O> batchOperationWraper, SearchReturnDataType returnDataType, int start, int count, int pageSize) throws SearchException {

        BatchOperation<O> batchOperation = null;
        if (batchOperationWraper != null) {
            batchOperation = (BatchOperation<O>) batchOperationWraper.getBatchOperation();
        }

        RelationalPathBase<Object> tableRelationalPath = buildTableRelationalPath(tableMapping);

		Predicate whereExp = (Predicate) expression.expression();
		Expression<?> attributesExp = buildSelectAttributes(attributes);

		SQLQuery<?> sqlSelectQuery = sqlQueryFactory.select(attributesExp).from(tableRelationalPath).where(whereExp);
		sqlSelectQuery.setUseLiterals(true);

        SQLQuery<?> baseQuery = sqlSelectQuery;
        if (orderBy != null) {
            baseQuery = sqlSelectQuery.orderBy(orderBy);
        }

        List<ResultSet> searchResultList = new ArrayList<ResultSet>();

        String queryStr = null;
        if ((SearchReturnDataType.SEARCH == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
        	ResultSet lastResult = null;
	        if (pageSize > 0) {
	            boolean collectSearchResult;
	
	            SQLQuery<?> query = null;
	            int currentLimit;
	    		try (Connection connection = sqlQueryFactory.getConnection()) {
	    			List<ResultSet> lastSearchResultList;
	                int resultCount = 0;
	                int lastCountRows = 0;
	                do {
	                    collectSearchResult = true;
	
	                    currentLimit = pageSize;
	                    if (count > 0) {
	                        currentLimit = Math.min(pageSize, count - resultCount);
	                    }
	
	                    query = baseQuery.limit(currentLimit).offset(start + resultCount);

	                    queryStr = sqlSelectQuery.getSQL().getSQL();
	                    LOG.debug("Execution query: '" + queryStr + "'");

	                    PreparedStatement pstmt = connection.prepareStatement(queryStr, Statement.RETURN_GENERATED_KEYS);
		    			lastResult = pstmt.executeQuery();
	
                    	lastResult.last();
		    			lastCountRows = lastResult.getRow();
                    	lastResult.first();
		    			
	                    if (batchOperation != null) {
	                        collectSearchResult = batchOperation.collectSearchResult(lastCountRows);
	                    }
	                    if (collectSearchResult) {
	                        searchResultList.add(lastResult);
	                    }
	
	                    if (batchOperation != null) {
	                    	lastResult.first();
	                        List<O> entries = batchOperationWraper.createEntities(lastResult);
	                        batchOperation.performAction(entries);
	                    }
	
	                    resultCount += lastCountRows;
	
	                    if ((count > 0) && (resultCount >= count)) {
	                        break;
	                    }
	                } while (lastCountRows > 0);
        		} catch (QueryException ex) {
        			throw new SearchException(String.format("Failed to build search entries query. Key: '%s', expression: '%s'", key, expression.expression()), ex);
	    		} catch (SQLException ex) {
	    			throw new SearchException(String.format("Failed to execute lookup query '%s'  with key: '%s'", queryStr, key), ex);
	    		}
	        } else {
	    		try (Connection connection = sqlQueryFactory.getConnection()) {
	                SQLQuery<?> query = baseQuery;
	                if (count > 0) {
	                    query = query.limit(count);
	                }
	                if (start > 0) {
	                    query = query.offset(start);
	                }
	
                    queryStr = sqlSelectQuery.getSQL().getSQL();
                    LOG.debug("Execution query: '" + queryStr + "'");

                    PreparedStatement pstmt = connection.prepareStatement(queryStr, Statement.RETURN_GENERATED_KEYS);
	    			lastResult = pstmt.executeQuery();
	
	                searchResultList.add(lastResult);
        		} catch (QueryException ex) {
        			throw new SearchException(String.format("Failed to build search entries query. Key: '%s', expression: '%s'", key, expression.expression()), ex);
	            } catch (SQLException ex) {
	                throw new SearchException("Failed to search entries. Query: '" + queryStr + "'", ex);
	            }
	        }
        }

        int resultSize = 0;
        for (ResultSet result : searchResultList) {
        	try {
				result.last();
			} catch (SQLException ex) {
                throw new SearchException("Failed to calculate count result entries. Query: '" + queryStr + "'", ex);
			}
        }

        PagedResult<ResultSet> result = new PagedResult<ResultSet>();
        result.setEntries(searchResultList);
        result.setEntriesCount(resultSize);
        result.setStart(start);

        if ((SearchReturnDataType.COUNT == returnDataType) || (SearchReturnDataType.SEARCH_COUNT == returnDataType)) {
    		SQLQuery<?> sqlCountSelectQuery = sqlQueryFactory.select(Expressions.as(ExpressionUtils.count(Wildcard.all), "TOTAL")).from(tableRelationalPath).where(whereExp);
    		sqlCountSelectQuery.setUseLiterals(true);

    		try (Connection connection = sqlQueryFactory.getConnection()) {
                queryStr = sqlCountSelectQuery.getSQL().getSQL();
                LOG.debug("Calculating count. Execution query: '" + queryStr + "'");

                PreparedStatement pstmt = connection.prepareStatement(queryStr, Statement.RETURN_GENERATED_KEYS);
                ResultSet countResult = pstmt.executeQuery();

                result.setTotalEntriesCount(countResult.getInt("TOTAL"));
    		} catch (QueryException ex) {
    			throw new SearchException(String.format("Failed to build count search entries query. Key: '%s', expression: '%s'", key, expression.expression()), ex);
            } catch (SQLException ex) {
                throw new SearchException("Failed to calculate count entries. Query: '" + queryStr + "'", ex);
            }
        }

        return result;
    }

    public String[] createStoragePassword(String[] passwords) {
        if (ArrayHelper.isEmpty(passwords)) {
            return passwords;
        }

        String[] results = new String[passwords.length];
        for (int i = 0; i < passwords.length; i++) {
			if (persistenceExtension == null) {
				results[i] = PasswordEncryptionHelper.createStoragePassword(passwords[i], connectionProvider.getPasswordEncryptionMethod());
			} else {
				results[i] = persistenceExtension.createHashedPassword(passwords[i]);
			}
        }

        return results;
    }

    @Override
    public boolean isBinaryAttribute(String attribute) {
        return this.connectionProvider.isBinaryAttribute(attribute);
    }

    @Override
    public boolean isCertificateAttribute(String attribute) {
        return this.connectionProvider.isCertificateAttribute(attribute);
    }

    public boolean isDisableAttributeMapping() {
		return disableAttributeMapping;
	}

	@Override
    public boolean destroy() {
        boolean result = true;

        if (connectionProvider != null) {
            try {
                connectionProvider.destory();
            } catch (Exception ex) {
                LOG.error("Failed to destory provider correctly");
                result = false;
            }
        }

        return result;
    }

    @Override
    public boolean isConnected() {
        return connectionProvider.isConnected();
    }

	@Override
	public void setPersistenceExtension(PersistenceExtension persistenceExtension) {
		this.persistenceExtension = persistenceExtension;
	}

	private Expression<?> buildSelectAttributes(String ... attributes) {
		if (ArrayHelper.isEmpty(attributes)) {
			return Expressions.list(Wildcard.all, Expressions.path(Object.class, docAlias, DOC_ID));
		} else if ((attributes.length == 1) && StringHelper.isEmpty(attributes[0])) {
        	// Compatibility with base persistence layer when application pass filter new String[] { "" }
			return Expressions.list(Expressions.path(Object.class, docAlias, DN), Expressions.path(Object.class, docAlias, DOC_ID));
		}
		
		List<Expression<?>> expresisons = new ArrayList<Expression<?>>(attributes.length + 2);
		
        boolean hasDn = false;
		for (String attribute : attributes) {
			expresisons.add(Expressions.path(Object.class, docAlias, attribute));

			hasDn &= StringHelper.equals(attribute, SqlOperationService.DN);
		}

		if (!hasDn) {
			expresisons.add(Expressions.path(Object.class, docAlias, DN));
		}

		expresisons.add(Expressions.path(Object.class, docAlias, DOC_ID));

		return Expressions.list(expresisons.toArray(new Expression<?>[0]));
	}

	private RelationalPathBase<Object> buildTableRelationalPath(TableMapping tableMapping) {
		RelationalPathBase<Object> tableRelationalPath = new RelationalPathBase<>(Object.class, DOC_ALIAS, this.schemaName, tableMapping.getTableName());

		return tableRelationalPath;
	}

}
