/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.spi.security;

import com.facebook.presto.common.CatalogSchemaName;
import com.facebook.presto.common.QualifiedObjectName;
import com.facebook.presto.spi.CatalogSchemaTableName;
import com.facebook.presto.spi.ColumnMetadata;
import com.facebook.presto.spi.MaterializedViewDefinition;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.analyzer.ViewDefinition;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.facebook.presto.spi.security.AccessDeniedException.denyAddColumn;
import static com.facebook.presto.spi.security.AccessDeniedException.denyAddConstraint;
import static com.facebook.presto.spi.security.AccessDeniedException.denyCatalogAccess;
import static com.facebook.presto.spi.security.AccessDeniedException.denyCreateSchema;
import static com.facebook.presto.spi.security.AccessDeniedException.denyCreateTable;
import static com.facebook.presto.spi.security.AccessDeniedException.denyCreateView;
import static com.facebook.presto.spi.security.AccessDeniedException.denyCreateViewWithSelect;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDeleteTable;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropColumn;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropConstraint;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropSchema;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropTable;
import static com.facebook.presto.spi.security.AccessDeniedException.denyDropView;
import static com.facebook.presto.spi.security.AccessDeniedException.denyGrantTablePrivilege;
import static com.facebook.presto.spi.security.AccessDeniedException.denyInsertTable;
import static com.facebook.presto.spi.security.AccessDeniedException.denyRenameColumn;
import static com.facebook.presto.spi.security.AccessDeniedException.denyRenameSchema;
import static com.facebook.presto.spi.security.AccessDeniedException.denyRenameTable;
import static com.facebook.presto.spi.security.AccessDeniedException.denyRenameView;
import static com.facebook.presto.spi.security.AccessDeniedException.denyRevokeTablePrivilege;
import static com.facebook.presto.spi.security.AccessDeniedException.denySelectColumns;
import static com.facebook.presto.spi.security.AccessDeniedException.denySetCatalogSessionProperty;
import static com.facebook.presto.spi.security.AccessDeniedException.denySetTableProperties;
import static com.facebook.presto.spi.security.AccessDeniedException.denyShowColumnsMetadata;
import static com.facebook.presto.spi.security.AccessDeniedException.denyShowCreateTable;
import static com.facebook.presto.spi.security.AccessDeniedException.denyShowSchemas;
import static com.facebook.presto.spi.security.AccessDeniedException.denyShowTablesMetadata;
import static com.facebook.presto.spi.security.AccessDeniedException.denyUpdateTableColumns;

public interface SystemAccessControl
{
    /**
     * Check if the principal is allowed to be the specified user.
     *
     * @throws AccessDeniedException if not allowed
     */
    void checkCanSetUser(Identity identity, AccessControlContext context, Optional<Principal> principal, String userName);

    default AuthorizedIdentity selectAuthorizedIdentity(Identity identity, AccessControlContext context, String userName, List<X509Certificate> certificates)
    {
        return new AuthorizedIdentity(userName, "", true);
    }

    /**
     * Check if the query is unexpectedly modified using the credentials passed in the identity.
     *
     * @throws AccessDeniedException if query is modified.
     */
    void checkQueryIntegrity(Identity identity, AccessControlContext context, String query, Map<QualifiedObjectName, ViewDefinition> viewDefinitions, Map<QualifiedObjectName, MaterializedViewDefinition> materializedViewDefinitions);

    /**
     * Check if identity is allowed to set the specified system property.
     *
     * @throws AccessDeniedException if not allowed
     */
    void checkCanSetSystemSessionProperty(Identity identity, AccessControlContext context, String propertyName);

    /**
     * Check if identity is allowed to access the specified catalog
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanAccessCatalog(Identity identity, AccessControlContext context, String catalogName)
    {
        denyCatalogAccess(catalogName);
    }

    /**
     * Filter the list of catalogs to those visible to the identity.
     */
    default Set<String> filterCatalogs(Identity identity, AccessControlContext context, Set<String> catalogs)
    {
        return Collections.emptySet();
    }

    /**
     * Check if identity is allowed to create the specified schema in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanCreateSchema(Identity identity, AccessControlContext context, CatalogSchemaName schema)
    {
        denyCreateSchema(schema.toString());
    }

    /**
     * Check if identity is allowed to drop the specified schema in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanDropSchema(Identity identity, AccessControlContext context, CatalogSchemaName schema)
    {
        denyDropSchema(schema.toString());
    }

    /**
     * Check if identity is allowed to rename the specified schema in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanRenameSchema(Identity identity, AccessControlContext context, CatalogSchemaName schema, String newSchemaName)
    {
        denyRenameSchema(schema.toString(), newSchemaName);
    }

    /**
     * Check if identity is allowed to execute SHOW SCHEMAS in a catalog.
     * <p>
     * NOTE: This method is only present to give users an error message when listing is not allowed.
     * The {@link #filterSchemas} method must filter all results for unauthorized users,
     * since there are multiple ways to list schemas.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanShowSchemas(Identity identity, AccessControlContext context, String catalogName)
    {
        denyShowSchemas();
    }

    /**
     * Filter the list of schemas in a catalog to those visible to the identity.
     */
    default Set<String> filterSchemas(Identity identity, AccessControlContext context, String catalogName, Set<String> schemaNames)
    {
        return Collections.emptySet();
    }

    /**
     * Check if identity is allowed to execute SHOW CREATE TABLE or SHOW CREATE VIEW.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanShowCreateTable(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyShowCreateTable(table.toString());
    }

    /**
     * Check if identity is allowed to create the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanCreateTable(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyCreateTable(table.toString());
    }

    /**
     * Check if identity is allowed to alter properties to the specified table in a catalog.
     *
     * @throws AccessDeniedException if not allowed
     */
    default void checkCanSetTableProperties(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denySetTableProperties(table.toString());
    }

    /**
     * Check if identity is allowed to drop the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanDropTable(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyDropTable(table.toString());
    }

    /**
     * Check if identity is allowed to rename the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanRenameTable(Identity identity, AccessControlContext context, CatalogSchemaTableName table, CatalogSchemaTableName newTable)
    {
        denyRenameTable(table.toString(), newTable.toString());
    }

    /**
     * Check if identity is allowed to show metadata of tables by executing SHOW TABLES, SHOW GRANTS etc. in a catalog.
     * <p>
     * NOTE: This method is only present to give users an error message when listing is not allowed.
     * The {@link #filterTables} method must filter all results for unauthorized users,
     * since there are multiple ways to list tables.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanShowTablesMetadata(Identity identity, AccessControlContext context, CatalogSchemaName schema)
    {
        denyShowTablesMetadata(schema.toString());
    }

    /**
     * Filter the list of tables and views to those visible to the identity.
     */
    default Set<SchemaTableName> filterTables(Identity identity, AccessControlContext context, String catalogName, Set<SchemaTableName> tableNames)
    {
        return Collections.emptySet();
    }

    /**
     * Check if identity is allowed to show columns of tables by executing SHOW COLUMNS, DESCRIBE etc.
     * <p>
     * NOTE: This method is only present to give users an error message when listing is not allowed.
     * The {@link #filterColumns} method must filter all results for unauthorized users,
     * since there are multiple ways to list columns.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanShowColumnsMetadata(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyShowColumnsMetadata(table.toString());
    }

    /**
     * Filter the list of columns to those visible to the identity.
     */
    default List<ColumnMetadata> filterColumns(Identity identity, AccessControlContext context, CatalogSchemaTableName table, List<ColumnMetadata> columns)
    {
        return Collections.emptyList();
    }

    /**
     * Check if identity is allowed to add columns to the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanAddColumn(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyAddColumn(table.toString());
    }

    /**
     * Check if identity is allowed to drop columns from the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanDropColumn(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyDropColumn(table.toString());
    }

    /**
     * Check if identity is allowed to rename a column in the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanRenameColumn(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyRenameColumn(table.toString());
    }

    /**
     * Check if identity is allowed to select from the specified columns in a relation.  The column set can be empty.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanSelectFromColumns(Identity identity, AccessControlContext context, CatalogSchemaTableName table, Set<String> columns)
    {
        denySelectColumns(table.toString(), columns);
    }

    /**
     * Check if identity is allowed to insert into the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanInsertIntoTable(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyInsertTable(table.toString());
    }

    /**
     * Check if identity is allowed to delete from the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanDeleteFromTable(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyDeleteTable(table.toString());
    }

    /**
     * Check if identity is allowed to truncate the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanTruncateTable(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyDeleteTable(table.toString());
    }

    /**
     * Check if identity is allowed to update the supplied columns in the specified table in a catalog.
     *
     * @throws AccessDeniedException if not allowed
     */
    default void checkCanUpdateTableColumns(Identity identity, AccessControlContext context, CatalogSchemaTableName table, Set<String> updatedColumnNames)
    {
        denyUpdateTableColumns(table.toString(), updatedColumnNames);
    }

    /**
     * Check if identity is allowed to create the specified view in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanCreateView(Identity identity, AccessControlContext context, CatalogSchemaTableName view)
    {
        denyCreateView(view.toString());
    }

    /**
     * Check if identity is allowed to rename the specified view in a catalog.
     *
     * @throws AccessDeniedException if not allowed
     */
    default void checkCanRenameView(Identity identity, AccessControlContext context, CatalogSchemaTableName view, CatalogSchemaTableName newView)
    {
        denyRenameView(view.toString(), newView.toString());
    }

    /**
     * Check if identity is allowed to drop the specified view in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanDropView(Identity identity, AccessControlContext context, CatalogSchemaTableName view)
    {
        denyDropView(view.toString());
    }

    /**
     * Check if identity is allowed to create a view that selects from the specified columns in a relation.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanCreateViewWithSelectFromColumns(Identity identity, AccessControlContext context, CatalogSchemaTableName table, Set<String> columns)
    {
        denyCreateViewWithSelect(table.toString(), identity);
    }

    /**
     * Check if identity is allowed to set the specified property in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanSetCatalogSessionProperty(Identity identity, AccessControlContext context, String catalogName, String propertyName)
    {
        denySetCatalogSessionProperty(propertyName);
    }

    /**
     * Check if identity is allowed to grant the specified privilege to the grantee on the specified table.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanGrantTablePrivilege(Identity identity, AccessControlContext context, Privilege privilege, CatalogSchemaTableName table, PrestoPrincipal grantee, boolean withGrantOption)
    {
        denyGrantTablePrivilege(privilege.toString(), table.toString());
    }

    /**
     * Check if identity is allowed to revoke the specified privilege on the specified table from the revokee.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanRevokeTablePrivilege(Identity identity, AccessControlContext context, Privilege privilege, CatalogSchemaTableName table, PrestoPrincipal revokee, boolean grantOptionFor)
    {
        denyRevokeTablePrivilege(privilege.toString(), table.toString());
    }

    /**
     * Check if identity is allowed to drop constraints from the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanDropConstraint(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyDropConstraint(table.toString());
    }

    /**
     * Check if identity is allowed to add constraints to the specified table in a catalog.
     *
     * @throws com.facebook.presto.spi.security.AccessDeniedException if not allowed
     */
    default void checkCanAddConstraint(Identity identity, AccessControlContext context, CatalogSchemaTableName table)
    {
        denyAddConstraint(table.toString());
    }

    /**
     * Get row filters associated with the given table and identity.
     * <p>
     * Each filter must be a scalar SQL expression of boolean type over the columns in the table.
     *
     * @return a list of filters, or empty list if not applicable
     */
    default List<ViewExpression> getRowFilters(Identity identity, AccessControlContext context, CatalogSchemaTableName tableName)
    {
        return Collections.emptyList();
    }

    /**
     * Bulk method for getting column masks for a subset of columns in a table.
     * <p>
     * Each mask must be a scalar SQL expression of a type coercible to the type of the column being masked. The expression
     * must be written in terms of columns in the table.
     *
     * @return a mapping from columns to masks, or an empty map if not applicable. The keys of the return Map are a subset of {@code columns}.
     */
    default Map<ColumnMetadata, ViewExpression> getColumnMasks(Identity identity, AccessControlContext context, CatalogSchemaTableName tableName, List<ColumnMetadata> columns)
    {
        return Collections.emptyMap();
    }
}
