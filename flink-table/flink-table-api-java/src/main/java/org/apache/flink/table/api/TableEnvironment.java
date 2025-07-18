/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.table.api;

import org.apache.flink.annotation.Experimental;
import org.apache.flink.annotation.PublicEvolving;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.annotation.ArgumentTrait;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.table.api.internal.TableEnvironmentImpl;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.catalog.CatalogModel;
import org.apache.flink.table.catalog.CatalogStore;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.functions.ProcessTableFunction;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.table.module.Module;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.resource.ResourceUri;
import org.apache.flink.table.types.AbstractDataType;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A table environment is the base class, entry point, and central context for creating Table and
 * SQL API programs.
 *
 * <p>It is unified both on a language level for all JVM-based languages (i.e. there is no
 * distinction between Scala and Java API) and for bounded and unbounded data processing.
 *
 * <p>A table environment is responsible for:
 *
 * <ul>
 *   <li>Connecting to external systems.
 *   <li>Registering and retrieving {@link Table}s and other meta objects from a catalog.
 *   <li>Executing SQL statements.
 *   <li>Offering further configuration options.
 * </ul>
 *
 * <p>The syntax for path in methods such as {@link #createTemporaryView(String, Table)} is
 * following {@code [[catalog-name.]database-name.]object-name}, where the catalog name and database
 * are optional. For path resolution see {@link #useCatalog(String)} and {@link
 * #useDatabase(String)}.
 *
 * <p>Example: {@code `cat.1`.`db`.`Table`} resolves to an object named 'Table' in a catalog named
 * 'cat.1' and database named 'db'.
 *
 * <p>Note: This environment is meant for pure table programs. If you would like to convert from or
 * to other Flink APIs, it might be necessary to use one of the available language-specific table
 * environments in the corresponding bridging modules.
 */
@PublicEvolving
public interface TableEnvironment {

    /**
     * Creates a table environment that is the entry point and central context for creating Table
     * and SQL API programs.
     *
     * <p>It is unified both on a language level for all JVM-based languages (i.e. there is no
     * distinction between Scala and Java API) and for bounded and unbounded data processing.
     *
     * <p>A table environment is responsible for:
     *
     * <ul>
     *   <li>Connecting to external systems.
     *   <li>Registering and retrieving {@link Table}s and other meta objects from a catalog.
     *   <li>Executing SQL statements.
     *   <li>Offering further configuration options.
     * </ul>
     *
     * <p>Note: This environment is meant for pure table programs. If you would like to convert from
     * or to other Flink APIs, it might be necessary to use one of the available language-specific
     * table environments in the corresponding bridging modules.
     *
     * @param settings The environment settings used to instantiate the {@link TableEnvironment}.
     */
    static TableEnvironment create(EnvironmentSettings settings) {
        return TableEnvironmentImpl.create(settings);
    }

    /**
     * Creates a table environment that is the entry point and central context for creating Table
     * and SQL API programs.
     *
     * <p>It is unified both on a language level for all JVM-based languages (i.e. there is no
     * distinction between Scala and Java API) and for bounded and unbounded data processing.
     *
     * <p>A table environment is responsible for:
     *
     * <ul>
     *   <li>Connecting to external systems.
     *   <li>Registering and retrieving {@link Table}s and other meta objects from a catalog.
     *   <li>Executing SQL statements.
     *   <li>Offering further configuration options.
     * </ul>
     *
     * <p>Note: This environment is meant for pure table programs. If you would like to convert from
     * or to other Flink APIs, it might be necessary to use one of the available language-specific
     * table environments in the corresponding bridging modules.
     *
     * @param configuration The specified options are used to instantiate the {@link
     *     TableEnvironment}.
     */
    static TableEnvironment create(Configuration configuration) {
        return TableEnvironmentImpl.create(configuration);
    }

    /**
     * Creates a Table from given values.
     *
     * <p>Examples:
     *
     * <p>You can use a {@code row(...)} expression to create a composite rows:
     *
     * <pre>{@code
     * tEnv.fromValues(
     *     row(1, "ABC"),
     *     row(2L, "ABCDE")
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     * root
     * |-- f0: BIGINT NOT NULL     // original types INT and BIGINT are generalized to BIGINT
     * |-- f1: VARCHAR(5) NOT NULL // original types CHAR(3) and CHAR(5) are generalized to VARCHAR(5)
     *                             // it uses VARCHAR instead of CHAR so that no padding is applied
     * }</pre>
     *
     * <p>The method will derive the types automatically from the input expressions. If types at a
     * certain position differ, the method will try to find a common super type for all types. If a
     * common super type does not exist, an exception will be thrown. If you want to specify the
     * requested type explicitly see {@link #fromValues(AbstractDataType, Object...)}.
     *
     * <p>It is also possible to use {@link org.apache.flink.types.Row} object instead of {@code
     * row} expressions.
     *
     * <p>ROWs that are a result of e.g. a function call are not flattened
     *
     * <pre>{@code
     * public class RowFunction extends ScalarFunction {
     *     {@literal @}DataTypeHint("ROW<f0 BIGINT, f1 VARCHAR(5)>")
     *     Row eval();
     * }
     *
     * tEnv.fromValues(
     *     call(new RowFunction()),
     *     call(new RowFunction())
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     * root
     * |-- f0: ROW<`f0` BIGINT, `f1` VARCHAR(5)>
     * }</pre>
     *
     * <p>The row constructor can be dropped to create a table with a single column:
     *
     * <p>ROWs that are a result of e.g. a function call are not flattened
     *
     * <pre>{@code
     * tEnv.fromValues(
     *     1,
     *     2L,
     *     3
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     * root
     * |-- f0: BIGINT NOT NULL
     * }</pre>
     *
     * @param values Expressions for constructing rows of the VALUES table.
     */
    default Table fromValues(Object... values) {
        // It is necessary here to implement TableEnvironment#fromValues(Object...) for
        // BatchTableEnvImpl.
        // In scala varargs are translated to Seq. Due to the type erasure Seq<Expression> and
        // Seq<Object>
        // are the same. It is not a problem in java as varargs in java are translated to an array.
        return fromValues(Arrays.asList(values));
    }

    /**
     * Creates a Table from given collection of objects with a given row type.
     *
     * <p>The difference between this method and {@link #fromValues(Object...)} is that the schema
     * can be manually adjusted. It might be helpful for assigning more generic types like e.g.
     * DECIMAL or naming the columns.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * tEnv.fromValues(
     *     DataTypes.ROW(
     *         DataTypes.FIELD("id", DataTypes.DECIMAL(10, 2)),
     *         DataTypes.FIELD("name", DataTypes.STRING())
     *     ),
     *     row(1, "ABC"),
     *     row(2L, "ABCDE")
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     * root
     * |-- id: DECIMAL(10, 2)
     * |-- name: STRING
     * }</pre>
     *
     * <p>For more examples see {@link #fromValues(Object...)}.
     *
     * @param rowType Expected row type for the values.
     * @param values Expressions for constructing rows of the VALUES table.
     * @see #fromValues(Object...)
     */
    default Table fromValues(AbstractDataType<?> rowType, Object... values) {
        // It is necessary here to implement TableEnvironment#fromValues(Object...) for
        // BatchTableEnvImpl.
        // In scala varargs are translated to Seq. Due to the type erasure Seq<Expression> and
        // Seq<Object>
        // are the same. It is not a problem in java as varargs in java are translated to an array.
        return fromValues(rowType, Arrays.asList(values));
    }

    /**
     * Creates a Table from given values.
     *
     * <p>Examples:
     *
     * <p>You can use a {@code row(...)} expression to create a composite rows:
     *
     * <pre>{@code
     * tEnv.fromValues(
     *     row(1, "ABC"),
     *     row(2L, "ABCDE")
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     *  root
     *  |-- f0: BIGINT NOT NULL     // original types INT and BIGINT are generalized to BIGINT
     *  |-- f1: VARCHAR(5) NOT NULL // original types CHAR(3) and CHAR(5) are generalized to VARCHAR(5)
     * 	 *                          // it uses VARCHAR instead of CHAR so that no padding is applied
     * }</pre>
     *
     * <p>The method will derive the types automatically from the input expressions. If types at a
     * certain position differ, the method will try to find a common super type for all types. If a
     * common super type does not exist, an exception will be thrown. If you want to specify the
     * requested type explicitly see {@link #fromValues(AbstractDataType, Expression...)}.
     *
     * <p>It is also possible to use {@link org.apache.flink.types.Row} object instead of {@code
     * row} expressions.
     *
     * <p>ROWs that are a result of e.g. a function call are not flattened
     *
     * <pre>{@code
     * public class RowFunction extends ScalarFunction {
     *     {@literal @}DataTypeHint("ROW<f0 BIGINT, f1 VARCHAR(5)>")
     *     Row eval();
     * }
     *
     * tEnv.fromValues(
     *     call(new RowFunction()),
     *     call(new RowFunction())
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     * root
     * |-- f0: ROW<`f0` BIGINT, `f1` VARCHAR(5)>
     * }</pre>
     *
     * <p>The row constructor can be dropped to create a table with a single column:
     *
     * <p>ROWs that are a result of e.g. a function call are not flattened
     *
     * <pre>{@code
     * tEnv.fromValues(
     *     lit(1).plus(2),
     *     lit(2L),
     *     lit(3)
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     * root
     * |-- f0: BIGINT NOT NULL
     * }</pre>
     *
     * @param values Expressions for constructing rows of the VALUES table.
     */
    Table fromValues(Expression... values);

    /**
     * Creates a Table from given collection of objects with a given row type.
     *
     * <p>The difference between this method and {@link #fromValues(Expression...)} is that the
     * schema can be manually adjusted. It might be helpful for assigning more generic types like
     * e.g. DECIMAL or naming the columns.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * tEnv.fromValues(
     *     DataTypes.ROW(
     *         DataTypes.FIELD("id", DataTypes.DECIMAL(10, 2)),
     *         DataTypes.FIELD("name", DataTypes.STRING())
     *     ),
     *     row(1, "ABC"),
     *     row(2L, "ABCDE")
     * )
     * }</pre>
     *
     * <p>will produce a Table with a schema as follows:
     *
     * <pre>{@code
     * root
     * |-- id: DECIMAL(10, 2)
     * |-- name: STRING
     * }</pre>
     *
     * <p>For more examples see {@link #fromValues(Expression...)}.
     *
     * @param rowType Expected row type for the values.
     * @param values Expressions for constructing rows of the VALUES table.
     * @see #fromValues(Expression...)
     */
    Table fromValues(AbstractDataType<?> rowType, Expression... values);

    /**
     * Creates a Table from given collection of objects.
     *
     * <p>See {@link #fromValues(Object...)} for more explanation.
     *
     * @param values Expressions for constructing rows of the VALUES table.
     * @see #fromValues(Object...)
     */
    Table fromValues(Iterable<?> values);

    /**
     * Creates a Table from given collection of objects with a given row type.
     *
     * <p>See {@link #fromValues(AbstractDataType, Object...)} for more explanation.
     *
     * @param rowType Expected row type for the values.
     * @param values Expressions for constructing rows of the VALUES table.
     * @see #fromValues(AbstractDataType, Object...)
     */
    Table fromValues(AbstractDataType<?> rowType, Iterable<?> values);

    /**
     * Registers a {@link Catalog} under a unique name. All tables registered in the {@link Catalog}
     * can be accessed.
     *
     * @param catalogName The name under which the catalog will be registered.
     * @param catalog The catalog to register.
     * @deprecated Use {@link #createCatalog(String, CatalogDescriptor)} instead. The new method
     *     uses a {@link CatalogDescriptor} to initialize the catalog instance and store the {@link
     *     CatalogDescriptor} to the {@link CatalogStore}.
     */
    @Deprecated
    void registerCatalog(String catalogName, Catalog catalog);

    /**
     * Creates a {@link Catalog} using the provided {@link CatalogDescriptor}. All table registered
     * in the {@link Catalog} can be accessed. The {@link CatalogDescriptor} will be persisted into
     * the {@link CatalogStore}.
     *
     * @param catalogName The name under which the catalog will be created
     * @param catalogDescriptor The catalog descriptor for creating catalog
     */
    void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor);

    /**
     * Gets a registered {@link Catalog} by name.
     *
     * @param catalogName The name to look up the {@link Catalog}.
     * @return The requested catalog, empty if there is no registered catalog with given name.
     */
    Optional<Catalog> getCatalog(String catalogName);

    /**
     * Loads a {@link Module} under a unique name. Modules will be kept in the loaded order.
     * ValidationException is thrown when there is already a module with the same name.
     *
     * @param moduleName name of the {@link Module}
     * @param module the module instance
     */
    void loadModule(String moduleName, Module module);

    /**
     * Enable modules in use with declared name order. Modules that have been loaded but not exist
     * in names varargs will become unused.
     *
     * @param moduleNames module names to be used
     */
    void useModules(String... moduleNames);

    /**
     * Unloads a {@link Module} with given name. ValidationException is thrown when there is no
     * module with the given name.
     *
     * @param moduleName name of the {@link Module}
     */
    void unloadModule(String moduleName);

    /**
     * Registers a {@link ScalarFunction} under a unique name. Replaces already existing
     * user-defined functions under this name.
     *
     * @deprecated Use {@link #createTemporarySystemFunction(String, UserDefinedFunction)} instead.
     *     Please note that the new method also uses the new type system and reflective extraction
     *     logic. It might be necessary to update the function implementation as well. See the
     *     documentation of {@link ScalarFunction} for more information on the new function design.
     */
    @Deprecated
    void registerFunction(String name, ScalarFunction function);

    /**
     * Registers a {@link UserDefinedFunction} class as a temporary system function.
     *
     * <p>Compared to {@link #createTemporaryFunction(String, Class)}, system functions are
     * identified by a global name that is independent of the current catalog and current database.
     * Thus, this method allows to extend the set of built-in system functions like {@code TRIM},
     * {@code ABS}, etc.
     *
     * <p>Temporary functions can shadow permanent ones. If a permanent function under a given name
     * exists, it will be inaccessible in the current session. To make the permanent function
     * available again one can drop the corresponding temporary system function.
     *
     * @param name The name under which the function will be registered globally.
     * @param functionClass The function class containing the implementation.
     */
    void createTemporarySystemFunction(
            String name, Class<? extends UserDefinedFunction> functionClass);

    /**
     * Registers a {@link UserDefinedFunction} instance as a temporary system function.
     *
     * <p>Compared to {@link #createTemporarySystemFunction(String, Class)}, this method takes a
     * function instance that might have been parameterized before (e.g. through its constructor).
     * This might be useful for more interactive sessions. Make sure that the instance is {@link
     * Serializable}.
     *
     * <p>Compared to {@link #createTemporaryFunction(String, UserDefinedFunction)}, system
     * functions are identified by a global name that is independent of the current catalog and
     * current database. Thus, this method allows to extend the set of built-in system functions
     * like {@code TRIM}, {@code ABS}, etc.
     *
     * <p>Temporary functions can shadow permanent ones. If a permanent function under a given name
     * exists, it will be inaccessible in the current session. To make the permanent function
     * available again one can drop the corresponding temporary system function.
     *
     * @param name The name under which the function will be registered globally.
     * @param functionInstance The (possibly pre-configured) function instance containing the
     *     implementation.
     */
    void createTemporarySystemFunction(String name, UserDefinedFunction functionInstance);

    /**
     * Drops a temporary system function registered under the given name.
     *
     * <p>If a permanent function with the given name exists, it will be used from now on for any
     * queries that reference this name.
     *
     * @param name The name under which the function has been registered globally.
     * @return true if a function existed under the given name and was removed
     */
    boolean dropTemporarySystemFunction(String name);

    /**
     * Registers a {@link UserDefinedFunction} class as a catalog function in the given path.
     *
     * <p>Compared to system functions with a globally defined name, catalog functions are always
     * (implicitly or explicitly) identified by a catalog and database.
     *
     * <p>There must not be another function (temporary or permanent) registered under the same
     * path.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param functionClass The function class containing the implementation.
     */
    void createFunction(String path, Class<? extends UserDefinedFunction> functionClass);

    /**
     * Registers a {@link UserDefinedFunction} class as a catalog function in the given path.
     *
     * <p>Compared to system functions with a globally defined name, catalog functions are always
     * (implicitly or explicitly) identified by a catalog and database.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param functionClass The function class containing the implementation.
     * @param ignoreIfExists If a function exists under the given path and this flag is set, no
     *     operation is executed. An exception is thrown otherwise.
     */
    void createFunction(
            String path,
            Class<? extends UserDefinedFunction> functionClass,
            boolean ignoreIfExists);

    /**
     * Registers a {@link UserDefinedFunction} class as a catalog function in the given path by the
     * specific class name and user defined resource uri.
     *
     * <p>Compared to {@link #createFunction(String, Class)}, this method allows registering a user
     * defined function by only providing a full path class name and a list of resources that
     * contain the implementation of the function along with its dependencies. Users don't need to
     * initialize the function instance in advance. The resource file can be a local or remote JAR
     * file.
     *
     * <p>Compared to system functions with a globally defined name, catalog functions are always
     * (implicitly or explicitly) identified by a catalog and database.
     *
     * <p>There must not be another function (temporary or permanent) registered under the same
     * path.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param className The class name of UDF to be registered.
     * @param resourceUris The list of udf resource uris in local or remote.
     */
    void createFunction(String path, String className, List<ResourceUri> resourceUris);

    /**
     * Registers a {@link UserDefinedFunction} class as a catalog function in the given path by the
     * specific class name and user defined resource uri.
     *
     * <p>Compared to {@link #createFunction(String, Class)}, this method allows registering a user
     * defined function by only providing a full path class name and a list of resources that
     * contain the implementation of the function along with its dependencies. Users don't need to
     * initialize the function instance in advance. The resource file can be a local or remote JAR
     * file.
     *
     * <p>Compared to system functions with a globally defined name, catalog functions are always
     * (implicitly or explicitly) identified by a catalog and database.
     *
     * <p>There must not be another function (temporary or permanent) registered under the same
     * path.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param className The class name of UDF to be registered.
     * @param resourceUris The list of udf resource uris in local or remote.
     * @param ignoreIfExists If a function exists under the given path and this flag is set, no
     *     operation is executed. An exception is thrown otherwise.
     */
    void createFunction(
            String path, String className, List<ResourceUri> resourceUris, boolean ignoreIfExists);

    /**
     * Registers a {@link UserDefinedFunction} class as a temporary catalog function.
     *
     * <p>Compared to {@link #createTemporarySystemFunction(String, Class)} with a globally defined
     * name, catalog functions are always (implicitly or explicitly) identified by a catalog and
     * database.
     *
     * <p>Temporary functions can shadow permanent ones. If a permanent function under a given name
     * exists, it will be inaccessible in the current session. To make the permanent function
     * available again one can drop the corresponding temporary function.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param functionClass The function class containing the implementation.
     */
    void createTemporaryFunction(String path, Class<? extends UserDefinedFunction> functionClass);

    /**
     * Registers a {@link UserDefinedFunction} instance as a temporary catalog function.
     *
     * <p>Compared to {@link #createTemporaryFunction(String, Class)}, this method takes a function
     * instance that might have been parameterized before (e.g. through its constructor). This might
     * be useful for more interactive sessions. Make sure that the instance is {@link Serializable}.
     *
     * <p>Compared to {@link #createTemporarySystemFunction(String, UserDefinedFunction)} with a
     * globally defined name, catalog functions are always (implicitly or explicitly) identified by
     * a catalog and database.
     *
     * <p>Temporary functions can shadow permanent ones. If a permanent function under a given name
     * exists, it will be inaccessible in the current session. To make the permanent function
     * available again one can drop the corresponding temporary function.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param functionInstance The (possibly pre-configured) function instance containing the
     *     implementation.
     */
    void createTemporaryFunction(String path, UserDefinedFunction functionInstance);

    /**
     * Registers a {@link UserDefinedFunction} class as a temporary catalog function in the given
     * path by the specific class name and user defined resource uri.
     *
     * <p>Compared to {@link #createTemporaryFunction(String, Class)}, this method allows
     * registering a user defined function by only providing a full path class name and a list of
     * resources that contain the implementation of the function along with its dependencies. Users
     * don't need to initialize the function instance in advance. The resource file can be a local
     * or remote JAR file.
     *
     * <p>Compared to {@link #createTemporarySystemFunction(String, String, List)} with a globally
     * defined name, catalog functions are always (implicitly or explicitly) identified by a catalog
     * and database.
     *
     * <p>Temporary functions can shadow permanent ones. If a permanent function under a given name
     * exists, it will be inaccessible in the current session. To make the permanent function
     * available again one can drop the corresponding temporary function.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param className The class name of UDF to be registered.
     * @param resourceUris The list udf resource uri in local or remote.
     */
    void createTemporaryFunction(String path, String className, List<ResourceUri> resourceUris);

    /**
     * Registers a {@link UserDefinedFunction} class as a temporary system function by the specific
     * class name and user defined resource uri.
     *
     * <p>Compared to {@link #createTemporaryFunction(String, Class)}, this method allows
     * registering a user defined function by only providing a full path class name and a list of
     * resources that contain the implementation of the function along with its dependencies. Users
     * don't need to initialize the function instance in advance. The resource file can be a local
     * or remote JAR file.
     *
     * <p>Temporary functions can shadow permanent ones. If a permanent function under a given name
     * exists, it will be inaccessible in the current session. To make the permanent function
     * available again one can drop the corresponding temporary system function.
     *
     * @param name The name under which the function will be registered globally.
     * @param className The class name of UDF to be registered.
     * @param resourceUris The list of udf resource uris in local or remote.
     */
    void createTemporarySystemFunction(
            String name, String className, List<ResourceUri> resourceUris);

    /**
     * Drops a catalog function registered in the given path.
     *
     * @param path The path under which the function has been registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @return true if a function existed in the given path and was removed
     */
    boolean dropFunction(String path);

    /**
     * Drops a temporary catalog function registered in the given path.
     *
     * <p>If a permanent function with the given path exists, it will be used from now on for any
     * queries that reference this path.
     *
     * @param path The path under which the function will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @return true if a function existed in the given path and was removed
     */
    boolean dropTemporaryFunction(String path);

    /**
     * Registers the given {@link TableDescriptor} as a temporary catalog table.
     *
     * <p>The {@link TableDescriptor descriptor} is converted into a {@link CatalogTable} and stored
     * in the catalog.
     *
     * <p>Temporary objects can shadow permanent ones. If a permanent object in a given path exists,
     * it will be inaccessible in the current session. To make the permanent object available again
     * one can drop the corresponding temporary object.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * tEnv.createTemporaryTable("MyTable", TableDescriptor.forConnector("datagen")
     *   .schema(Schema.newBuilder()
     *     .column("f0", DataTypes.STRING())
     *     .build())
     *   .option(DataGenOptions.ROWS_PER_SECOND, 10)
     *   .option("fields.f0.kind", "random")
     *   .build());
     * }</pre>
     *
     * @param path The path under which the table will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor Template for creating a {@link CatalogTable} instance.
     */
    void createTemporaryTable(String path, TableDescriptor descriptor);

    /**
     * Registers the given {@link TableDescriptor} as a temporary catalog table.
     *
     * <p>The {@link TableDescriptor descriptor} is converted into a {@link CatalogTable} and stored
     * in the catalog.
     *
     * <p>Temporary objects can shadow permanent ones. If a permanent object in a given path exists,
     * it will be inaccessible in the current session. To make the permanent object available again
     * one can drop the corresponding temporary object.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * tEnv.createTemporaryTable("MyTable", TableDescriptor.forConnector("datagen")
     *   .schema(Schema.newBuilder()
     *     .column("f0", DataTypes.STRING())
     *     .build())
     *   .option(DataGenOptions.ROWS_PER_SECOND, 10)
     *   .option("fields.f0.kind", "random")
     *   .build(),
     *  true);
     * }</pre>
     *
     * @param path The path under which the table will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor Template for creating a {@link CatalogTable} instance.
     * @param ignoreIfExists If a table exists under the given path and this flag is set, no
     *     operation is executed. An exception is thrown otherwise.
     */
    void createTemporaryTable(String path, TableDescriptor descriptor, boolean ignoreIfExists);

    /**
     * Registers the given {@link TableDescriptor} as a catalog table.
     *
     * <p>The {@link TableDescriptor descriptor} is converted into a {@link CatalogTable} and stored
     * in the catalog.
     *
     * <p>If the table should not be permanently stored in a catalog, use {@link
     * #createTemporaryTable(String, TableDescriptor)} instead.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * tEnv.createTable("MyTable", TableDescriptor.forConnector("datagen")
     *   .schema(Schema.newBuilder()
     *     .column("f0", DataTypes.STRING())
     *     .build())
     *   .option(DataGenOptions.ROWS_PER_SECOND, 10)
     *   .option("fields.f0.kind", "random")
     *   .build());
     * }</pre>
     *
     * @param path The path under which the table will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor Template for creating a {@link CatalogTable} instance.
     */
    void createTable(String path, TableDescriptor descriptor);

    /**
     * Registers the given {@link TableDescriptor} as a catalog table.
     *
     * <p>The {@link TableDescriptor descriptor} is converted into a {@link CatalogTable} and stored
     * in the catalog.
     *
     * <p>If the table should not be permanently stored in a catalog, use {@link
     * #createTemporaryTable(String, TableDescriptor, boolean)} instead.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * tEnv.createTable("MyTable", TableDescriptor.forConnector("datagen")
     *   .schema(Schema.newBuilder()
     *     .column("f0", DataTypes.STRING())
     *     .build())
     *   .option(DataGenOptions.ROWS_PER_SECOND, 10)
     *   .option("fields.f0.kind", "random")
     *   .build(),
     *  true);
     * }</pre>
     *
     * @param path The path under which the table will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor Template for creating a {@link CatalogTable} instance.
     * @param ignoreIfExists If a table exists under the given path and this flag is set, no
     *     operation is executed. An exception is thrown otherwise.
     * @return true if table was created in the given path, false if a permanent object already
     *     exists in the given path.
     */
    boolean createTable(String path, TableDescriptor descriptor, boolean ignoreIfExists);

    /**
     * Registers a {@link Table} under a unique name in the TableEnvironment's catalog. Registered
     * tables can be referenced in SQL queries.
     *
     * <p>Temporary objects can shadow permanent ones. If a permanent object in a given path exists,
     * it will be inaccessible in the current session. To make the permanent object available again
     * one can drop the corresponding temporary object.
     *
     * @param name The name under which the table will be registered.
     * @param table The table to register.
     * @deprecated use {@link #createTemporaryView(String, Table)}
     */
    @Deprecated
    void registerTable(String name, Table table);

    /**
     * Registers a {@link Table} API object as a temporary view similar to SQL temporary views.
     *
     * <p>Temporary objects can shadow permanent ones. If a permanent object in a given path exists,
     * it will be inaccessible in the current session. To make the permanent object available again
     * one can drop the corresponding temporary object.
     *
     * @param path The path under which the view will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param view The view to register.
     */
    void createTemporaryView(String path, Table view);

    /**
     * Registers a {@link Table} API object as a view similar to SQL views.
     *
     * <p>Temporary objects can shadow permanent ones. If a temporary object in a given path exists,
     * the permanent one will be inaccessible in the current session. To make the permanent object
     * available again one can drop the corresponding temporary object.
     *
     * @param path The path under which the view will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param view The view to register.
     */
    void createView(String path, Table view);

    /**
     * Registers a {@link Table} API object as a view similar to SQL views.
     *
     * <p>Temporary objects can shadow permanent ones. If a temporary object in a given path exists,
     * the permanent one will be inaccessible in the current session. To make the permanent object
     * available again one can drop the corresponding temporary object.
     *
     * @param path The path under which the view will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param view The view to register.
     * @param ignoreIfExists If a view or a table exists and the given flag is set, no operation is
     *     executed. An exception is thrown otherwise.
     * @return true if view was created in the given path, false if a permanent object already
     *     exists in the given path.
     */
    boolean createView(String path, Table view, boolean ignoreIfExists);

    /**
     * Registers the given {@link ModelDescriptor} as a catalog model similar to SQL models.
     *
     * <p>The {@link ModelDescriptor descriptor} is converted into a {@link CatalogModel} and stored
     * in the catalog.
     *
     * <p>If the model should not be permanently stored in a catalog, use {@link
     * #createTemporaryModel(String, ModelDescriptor)} instead.
     *
     * <p>Temporary objects can shadow permanent ones. If a temporary object in a given path exists,
     * the permanent one will be inaccessible in the current session. To make the permanent object
     * available again one can drop the corresponding temporary object.
     *
     * @param path The path under which the model will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor The descriptor of the model to register.
     */
    void createModel(String path, ModelDescriptor descriptor);

    /**
     * Registers the given {@link ModelDescriptor} as a catalog model similar to SQL models.
     *
     * <p>The {@link ModelDescriptor descriptor} is converted into a {@link CatalogModel} and stored
     * in the catalog.
     *
     * <p>If the model should not be permanently stored in a catalog, use {@link
     * #createTemporaryModel(String, ModelDescriptor)} instead.
     *
     * <p>Temporary objects can shadow permanent ones. If a temporary object in a given path exists,
     * the permanent one will be inaccessible in the current session. To make the permanent object
     * available again one can drop the corresponding temporary object.
     *
     * @param path The path under which the model will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor Template for creating a {@link CatalogModel} instance.
     * @param ignoreIfExists If a model exists and the given flag is set, no operation is executed.
     *     An exception is thrown otherwise.
     */
    void createModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists);

    /**
     * Registers the given {@link ModelDescriptor} as a temporary catalog model similar to SQL
     * models.
     *
     * <p>The {@link ModelDescriptor descriptor} is converted into a {@link CatalogModel} and stored
     * in the catalog.
     *
     * <p>Temporary objects can shadow permanent ones. If a permanent object in a given path exists,
     * it will be inaccessible in the current session. To make the permanent object available again
     * one can drop the corresponding temporary object.
     *
     * @param path The path under which the model will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor Template for creating a {@link CatalogModel} instance.
     */
    void createTemporaryModel(String path, ModelDescriptor descriptor);

    /**
     * Registers the given {@link ModelDescriptor} as a temporary catalog model similar to SQL
     * models.
     *
     * <p>The {@link ModelDescriptor descriptor} is converted into a {@link CatalogModel} and stored
     * in the catalog.
     *
     * <p>Temporary objects can shadow permanent ones. If a permanent object in a given path exists,
     * it will be inaccessible in the current session. To make the permanent object available again
     * one can drop the corresponding temporary object.
     *
     * @param path The path under which the model will be registered. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param descriptor Template for creating a {@link CatalogModel} instance.
     * @param ignoreIfExists If a model exists and the given flag is set, no operation is executed.
     *     An exception is thrown otherwise.
     */
    void createTemporaryModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists);

    /**
     * Scans a registered table and returns the resulting {@link Table}.
     *
     * <p>A table to scan must be registered in the {@link TableEnvironment}. It can be either
     * directly registered or be an external member of a {@link Catalog}.
     *
     * <p>See the documentation of {@link TableEnvironment#useDatabase(String)} or {@link
     * TableEnvironment#useCatalog(String)} for the rules on the path resolution.
     *
     * <p>Examples:
     *
     * <p>Scanning a directly registered table.
     *
     * <pre>{@code
     * Table tab = tableEnv.scan("tableName");
     * }</pre>
     *
     * <p>Scanning a table from a registered catalog.
     *
     * <pre>{@code
     * Table tab = tableEnv.scan("catalogName", "dbName", "tableName");
     * }</pre>
     *
     * @param tablePath The path of the table to scan.
     * @return The resulting {@link Table}.
     * @see TableEnvironment#useCatalog(String)
     * @see TableEnvironment#useDatabase(String)
     * @deprecated use {@link #from(String)}
     */
    @Deprecated
    Table scan(String... tablePath);

    /**
     * Reads a registered table and returns the resulting {@link Table}.
     *
     * <p>A table to scan must be registered in the {@link TableEnvironment}.
     *
     * <p>See the documentation of {@link TableEnvironment#useDatabase(String)} or {@link
     * TableEnvironment#useCatalog(String)} for the rules on the path resolution.
     *
     * <p>Examples:
     *
     * <p>Reading a table from default catalog and database.
     *
     * <pre>{@code
     * Table tab = tableEnv.from("tableName");
     * }</pre>
     *
     * <p>Reading a table from a registered catalog.
     *
     * <pre>{@code
     * Table tab = tableEnv.from("catalogName.dbName.tableName");
     * }</pre>
     *
     * <p>Reading a table from a registered catalog with escaping. Dots in e.g. a database name must
     * be escaped.
     *
     * <pre>{@code
     * Table tab = tableEnv.from("catalogName.`db.Name`.Table");
     * }</pre>
     *
     * <p>Note that the returned {@link Table} is an API object and only contains a pipeline
     * description. It actually corresponds to a <i>view</i> in SQL terms. Call {@link
     * Table#execute()} to trigger an execution.
     *
     * @param path The path of a table API object to scan.
     * @return The {@link Table} object describing the pipeline for further transformations.
     * @see TableEnvironment#useCatalog(String)
     * @see TableEnvironment#useDatabase(String)
     */
    Table from(String path);

    /**
     * Returns a {@link Table} backed by the given {@link TableDescriptor descriptor}.
     *
     * <p>The {@link TableDescriptor descriptor} won't be registered in the catalog, but it will be
     * propagated directly in the operation tree. Note that calling this method multiple times, even
     * with the same descriptor, results in multiple temporary tables. In such cases, it is
     * recommended to register it under a name using {@link #createTemporaryTable(String,
     * TableDescriptor)} and reference it via {@link #from(String)}.
     *
     * <p>Examples:
     *
     * <pre>{@code
     * Table table = tEnv.from(TableDescriptor.forConnector("datagen")
     *   .schema(Schema.newBuilder()
     *     .column("f0", DataTypes.STRING())
     *     .build())
     *   .build());
     * }</pre>
     *
     * <p>Note that the returned {@link Table} is an API object and only contains a pipeline
     * description. It actually corresponds to a <i>view</i> in SQL terms. Call {@link
     * Table#execute()} to trigger an execution.
     *
     * @return The {@link Table} object describing the pipeline for further transformations.
     */
    Table from(TableDescriptor descriptor);

    /**
     * Returns a {@link Table} backed by a call to a process table function (PTF).
     *
     * <p>A PTF maps zero, one, or multiple tables to a new table. PTFs are the most powerful
     * function kind for Flink SQL and Table API. They enable implementing user-defined operators
     * that can be as feature-rich as built-in operations. PTFs have access to Flink's managed
     * state, event-time and timer services, underlying table changelogs, and can take multiple
     * partitioned tables to produce a new table.
     *
     * <p>This method assumes a call to a previously registered function.
     *
     * <p>Example:
     *
     * <pre>{@code
     * env.createFunction("MyPTF", MyPTF.class);
     *
     * Table table = env.fromCall(
     *   "MyPTF",
     *   table.partitionBy($("key")).asArgument("input_table"),
     *   lit("Bob").asArgument("default_name"),
     *   lit(42).asArgument("default_threshold")
     * );
     * }</pre>
     *
     * <p>A PTF can digest tables either per row (with row semantics) or per set (with set
     * semantics). For set semantics ({@link ArgumentTrait#SET_SEMANTIC_TABLE}), make sure to
     * partition the table first using {@link Table#partitionBy(Expression...)}.
     *
     * @param path The path of a function.
     * @param arguments Table and scalar argument {@link Expressions}.
     * @return The {@link Table} object describing the pipeline for further transformations.
     * @see Expressions#call(String, Object...)
     * @see ProcessTableFunction
     */
    Table fromCall(String path, Object... arguments);

    /**
     * Returns a {@link Table} backed by a call to a process table function (PTF).
     *
     * <p>A PTF maps zero, one, or multiple tables to a new table. PTFs are the most powerful
     * function kind for Flink SQL and Table API. They enable implementing user-defined operators
     * that can be as feature-rich as built-in operations. PTFs have access to Flink's managed
     * state, event-time and timer services, underlying table changelogs, and can take multiple
     * partitioned tables to produce a new table.
     *
     * <p>This method assumes a call to an unregistered, inline function.
     *
     * <p>Example:
     *
     * <pre>{@code
     * Table table = env.fromCall(
     *   MyPTF.class,
     *   table.partitionBy($("key")).asArgument("input_table")
     * );
     * }</pre>
     *
     * <p>A PTF can digest tables either per row (with row semantics) or per set (with set
     * semantics). For set semantics ({@link ArgumentTrait#SET_SEMANTIC_TABLE}), make sure to
     * partition the table first using {@link Table#partitionBy(Expression...)}.
     *
     * @param function The class containing the function's logic.
     * @param arguments Table and scalar argument {@link Expressions}.
     * @return The {@link Table} object describing the pipeline for further transformations.
     * @see Expressions#call(Class, Object...)
     * @see ProcessTableFunction
     */
    Table fromCall(Class<? extends UserDefinedFunction> function, Object... arguments);

    /**
     * Gets the names of all catalogs registered in this environment.
     *
     * @return A list of the names of all registered catalogs.
     */
    String[] listCatalogs();

    /**
     * Gets an array of names of all used modules in this environment in resolution order.
     *
     * @return A list of the names of used modules in resolution order.
     */
    String[] listModules();

    /**
     * Gets an array of all loaded modules with use status in this environment. Used modules are
     * kept in resolution order.
     *
     * @return A list of name and use status entries of all loaded modules.
     */
    ModuleEntry[] listFullModules();

    /**
     * Gets the names of all databases registered in the current catalog.
     *
     * @return A list of the names of all registered databases in the current catalog.
     */
    String[] listDatabases();

    /**
     * Gets the names of all tables available in the current namespace (the current database of the
     * current catalog). It returns both temporary and permanent tables and views.
     *
     * @return A list of the names of all registered tables in the current database of the current
     *     catalog.
     * @see #listTemporaryTables()
     * @see #listTemporaryViews()
     */
    String[] listTables();

    /**
     * Gets the names of all tables available in the given namespace (the given database of the
     * given catalog). It returns both temporary and permanent tables and views.
     *
     * @return A list of the names of all registered tables in the given database of the given
     *     catalog.
     * @see #listTemporaryTables()
     * @see #listTemporaryViews()
     */
    String[] listTables(String catalogName, String databaseName);

    /**
     * Gets the names of all views available in the current namespace (the current database of the
     * current catalog). It returns both temporary and permanent views.
     *
     * @return A list of the names of all registered views in the current database of the current
     *     catalog.
     * @see #listTemporaryViews()
     */
    String[] listViews();

    /**
     * Gets the names of all temporary tables and views available in the current namespace (the
     * current database of the current catalog).
     *
     * @return A list of the names of all registered temporary tables and views in the current
     *     database of the current catalog.
     * @see #listTables()
     */
    String[] listTemporaryTables();

    /**
     * Gets the names of all temporary views available in the current namespace (the current
     * database of the current catalog).
     *
     * @return A list of the names of all registered temporary views in the current database of the
     *     current catalog.
     * @see #listTables()
     */
    String[] listTemporaryViews();

    /** Gets the names of all user defined functions registered in this environment. */
    String[] listUserDefinedFunctions();

    /** Gets the names of all functions in this environment. */
    String[] listFunctions();

    /**
     * Gets the names of all models available in the current namespace (the current database of the
     * current catalog). It returns both temporary and permanent models.
     *
     * @return A list of the names of all registered models in the current database of the current
     *     catalog.
     * @see #listTemporaryModels()
     */
    String[] listModels();

    /**
     * Gets the names of all temporary Models available in the current namespace (the current
     * database of the current catalog).
     *
     * @return A list of the names of all registered temporary models in the current database of the
     *     current catalog.
     * @see #listModels()
     */
    String[] listTemporaryModels();

    /**
     * Drops a temporary table registered in the given path.
     *
     * <p>If a permanent table with a given path exists, it will be used from now on for any queries
     * that reference this path.
     *
     * @param path The given path under which the temporary table will be dropped. See also the
     *     {@link TableEnvironment} class description for the format of the path.
     * @return true if a table existed in the given path and was removed
     */
    boolean dropTemporaryTable(String path);

    /**
     * Drops a table registered in the given path.
     *
     * <p>This method can only drop permanent objects. Temporary objects can shadow permanent ones.
     * If a temporary object exists in a given path, make sure to drop the temporary object first
     * using {@link #dropTemporaryTable}.
     *
     * <p>Compared to SQL, this method will not throw an error if the table does not exist. Use
     * {@link #dropTable(java.lang.String, boolean)} to change the default behavior.
     *
     * @param path The given path under which the table will be dropped. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @return true if table existed in the given path and was dropped, false if table didn't exist
     *     in the given path.
     */
    boolean dropTable(String path);

    /**
     * Drops a table registered in the given path.
     *
     * <p>This method can only drop permanent objects. Temporary objects can shadow permanent ones.
     * If a temporary object exists in a given path, make sure to drop the temporary object first
     * using {@link #dropTemporaryTable}.
     *
     * @param path The given path under which the given table will be dropped. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param ignoreIfNotExists If false exception will be thrown if the view to drop does not
     *     exist.
     * @return true if table existed in the given path and was dropped, false if table didn't exist
     *     in the given path.
     */
    boolean dropTable(String path, boolean ignoreIfNotExists);

    /**
     * Drops a temporary view registered in the given path.
     *
     * <p>If a permanent table or view with a given path exists, it will be used from now on for any
     * queries that reference this path.
     *
     * @param path The given path under which the temporary view will be dropped. See also the
     *     {@link TableEnvironment} class description for the format of the path.
     * @return true if a view existed in the given path and was removed
     */
    boolean dropTemporaryView(String path);

    /**
     * Drops a view registered in the given path.
     *
     * <p>This method can only drop permanent objects. Temporary objects can shadow permanent ones.
     * If a temporary object exists in a given path, make sure to drop the temporary object first
     * using {@link #dropTemporaryView}.
     *
     * <p>Compared to SQL, this method will not throw an error if the view does not exist. Use
     * {@link #dropView(java.lang.String, boolean)} to change the default behavior.
     *
     * @param path The given path under which the view will be dropped. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @return true if view existed in the given path and was dropped, false if view didn't exist in
     *     the given path.
     */
    boolean dropView(String path);

    /**
     * Drops a view registered in the given path.
     *
     * <p>This method can only drop permanent objects. Temporary objects can shadow permanent ones.
     * If a temporary object exists in a given path, make sure to drop the temporary object first
     * using {@link #dropTemporaryView}.
     *
     * @param path The given path under which the view will be dropped. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param ignoreIfNotExists If false exception will be thrown if the view to drop does not
     *     exist.
     * @return true if view existed in the given path and was dropped, false if view didn't exist in
     *     the given path and ignoreIfNotExists was true.
     */
    boolean dropView(String path, boolean ignoreIfNotExists);

    /**
     * Drops a model registered in the given path.
     *
     * <p>This method can only drop permanent objects. Temporary objects can shadow permanent ones.
     * If a temporary object exists in a given path, make sure to drop the temporary object first
     * using {@link #dropTemporaryModel}.
     *
     * @param path The given path under which the model will be dropped. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @return true if model existed in the given path and was dropped, false if model didn't exist
     *     in the given path.
     */
    boolean dropModel(String path);

    /**
     * Drops a model registered in the given path.
     *
     * <p>This method can only drop permanent objects. Temporary objects can shadow permanent ones.
     * If a temporary object exists in a given path, make sure to drop the temporary object first
     * using {@link #dropTemporaryModel}.
     *
     * @param path The given path under which the model will be dropped. See also the {@link
     *     TableEnvironment} class description for the format of the path.
     * @param ignoreIfNotExists If false exception will be thrown if the model to drop does not
     *     exist.
     * @return true if model existed in the given path and was dropped, false if model didn't exist
     *     in the given path.
     */
    boolean dropModel(String path, boolean ignoreIfNotExists);

    /**
     * Drops a temporary model registered in the given path.
     *
     * <p>If a permanent model with a given path exists, it will be used from now on for any queries
     * that reference this path.
     *
     * @param path The given path under which the temporary model will be dropped. See also the
     *     {@link TableEnvironment} class description for the format of the path.
     * @return true if a model existed in the given path and was removed
     */
    boolean dropTemporaryModel(String path);

    /**
     * Returns the AST of the specified statement and the execution plan to compute the result of
     * the given statement.
     *
     * @param statement The statement for which the AST and execution plan will be returned.
     * @param extraDetails The extra explain details which the explain result should include, e.g.
     *     estimated cost, changelog mode for streaming, displaying execution plan in json format
     * @return AST and the execution plan.
     */
    default String explainSql(String statement, ExplainDetail... extraDetails) {
        return explainSql(statement, ExplainFormat.TEXT, extraDetails);
    }

    /**
     * Returns the AST of the specified statement and the execution plan to compute the result of
     * the given statement.
     *
     * @param statement The statement for which the AST and execution plan will be returned.
     * @param format The output format of explained plan.
     * @param extraDetails The extra explain details which the explain result should include, e.g.
     *     estimated cost, changelog mode for streaming, displaying execution plan in json format
     * @return AST and the execution plan.
     */
    String explainSql(String statement, ExplainFormat format, ExplainDetail... extraDetails);

    /**
     * Returns completion hints for the given statement at the given cursor position. The completion
     * happens case insensitively.
     *
     * @param statement Partial or slightly incorrect SQL statement
     * @param position cursor position
     * @return completion hints that fit at the current cursor position
     * @deprecated Will be removed in the next release
     */
    @Deprecated
    String[] getCompletionHints(String statement, int position);

    /**
     * Evaluates a SQL query on registered tables and returns a {@link Table} object describing the
     * pipeline for further transformations.
     *
     * <p>All tables and other objects referenced by the query must be registered in the {@link
     * TableEnvironment}. For example, use {@link #createTemporaryView(String, Table)}) for
     * referencing a {@link Table} object or {@link #createTemporarySystemFunction(String, Class)}
     * for functions.
     *
     * <p>Alternatively, a {@link Table} object is automatically registered when its {@link
     * Table#toString()} method is called, for example when it is embedded into a string. Hence, SQL
     * queries can directly reference a {@link Table} object inline (i.e. anonymous) as follows:
     *
     * <pre>{@code
     * Table table = ...;
     * String tableName = table.toString();
     * // the table is not registered to the table environment
     * tEnv.sqlQuery("SELECT * FROM " + tableName + " WHERE a > 12");
     * }</pre>
     *
     * <p>Note that the returned {@link Table} is an API object and only contains a pipeline
     * description. It actually corresponds to a <i>view</i> in SQL terms. Call {@link
     * Table#execute()} to trigger an execution or use {@link #executeSql(String)} directly.
     *
     * @param query The SQL query to evaluate.
     * @return The {@link Table} object describing the pipeline for further transformations.
     */
    Table sqlQuery(String query);

    /**
     * Executes the given single statement and returns the execution result.
     *
     * <p>The statement can be DDL/DML/DQL/SHOW/DESCRIBE/EXPLAIN/USE. For DML and DQL, this method
     * returns {@link TableResult} once the job has been submitted. For DDL and DCL statements,
     * {@link TableResult} is returned once the operation has finished.
     *
     * <p>If multiple pipelines should insert data into one or more sink tables as part of a single
     * execution, use a {@link StatementSet} (see {@link TableEnvironment#createStatementSet()}).
     *
     * <p>By default, all DML operations are executed asynchronously. Use {@link
     * TableResult#await()} or {@link TableResult#getJobClient()} to monitor the execution. Set
     * {@link TableConfigOptions#TABLE_DML_SYNC} for always synchronous execution.
     *
     * @return content for DQL/SHOW/DESCRIBE/EXPLAIN, the affected row count for `DML` (-1 means
     *     unknown), or a string message ("OK") for other statements.
     */
    TableResult executeSql(String statement);

    /**
     * Gets the current default catalog name of the current session.
     *
     * @return The current default catalog name that is used for the path resolution.
     * @see TableEnvironment#useCatalog(String)
     */
    String getCurrentCatalog();

    /**
     * Sets the current catalog to the given value. It also sets the default database to the
     * catalog's default one. See also {@link TableEnvironment#useDatabase(String)}.
     *
     * <p>This is used during the resolution of object paths. Both the catalog and database are
     * optional when referencing catalog objects such as tables, views etc. The algorithm looks for
     * requested objects in following paths in that order:
     *
     * <ol>
     *   <li>{@code [current-catalog].[current-database].[requested-path]}
     *   <li>{@code [current-catalog].[requested-path]}
     *   <li>{@code [requested-path]}
     * </ol>
     *
     * <p>Example:
     *
     * <p>Given structure with default catalog set to {@code default_catalog} and default database
     * set to {@code default_database}.
     *
     * <pre>
     * root:
     *   |- default_catalog
     *       |- default_database
     *           |- tab1
     *       |- db1
     *           |- tab1
     *   |- cat1
     *       |- db1
     *           |- tab1
     * </pre>
     *
     * <p>The following table describes resolved paths:
     *
     * <table>
     *     <thead>
     *         <tr>
     *             <th>Requested path</th>
     *             <th>Resolved path</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>tab1</td>
     *             <td>default_catalog.default_database.tab1</td>
     *         </tr>
     *         <tr>
     *             <td>db1.tab1</td>
     *             <td>default_catalog.db1.tab1</td>
     *         </tr>
     *         <tr>
     *             <td>cat1.db1.tab1</td>
     *             <td>cat1.db1.tab1</td>
     *         </tr>
     *     </tbody>
     * </table>
     *
     * <p>You can unset the current catalog by passing a null value. If the current catalog is
     * unset, you need to use fully qualified identifiers.
     *
     * @param catalogName The name of the catalog to set as the current default catalog.
     * @see TableEnvironment#useDatabase(String)
     */
    void useCatalog(@Nullable String catalogName);

    /**
     * Gets the current default database name of the running session.
     *
     * @return The name of the current database of the current catalog.
     * @see TableEnvironment#useDatabase(String)
     */
    String getCurrentDatabase();

    /**
     * Sets the current default database. It has to exist in the current catalog. That path will be
     * used as the default one when looking for unqualified object names.
     *
     * <p>This is used during the resolution of object paths. Both the catalog and database are
     * optional when referencing catalog objects such as tables, views etc. The algorithm looks for
     * requested objects in following paths in that order:
     *
     * <ol>
     *   <li>{@code [current-catalog].[current-database].[requested-path]}
     *   <li>{@code [current-catalog].[requested-path]}
     *   <li>{@code [requested-path]}
     * </ol>
     *
     * <p>Example:
     *
     * <p>Given structure with default catalog set to {@code default_catalog} and default database
     * set to {@code default_database}.
     *
     * <pre>
     * root:
     *   |- default_catalog
     *       |- default_database
     *           |- tab1
     *       |- db1
     *           |- tab1
     *   |- cat1
     *       |- db1
     *           |- tab1
     * </pre>
     *
     * <p>The following table describes resolved paths:
     *
     * <table>
     *     <thead>
     *         <tr>
     *             <th>Requested path</th>
     *             <th>Resolved path</th>
     *         </tr>
     *     </thead>
     *     <tbody>
     *         <tr>
     *             <td>tab1</td>
     *             <td>default_catalog.default_database.tab1</td>
     *         </tr>
     *         <tr>
     *             <td>db1.tab1</td>
     *             <td>default_catalog.db1.tab1</td>
     *         </tr>
     *         <tr>
     *             <td>cat1.db1.tab1</td>
     *             <td>cat1.db1.tab1</td>
     *         </tr>
     *     </tbody>
     * </table>
     *
     * <p>You can unset the current database by passing a null value. If the current database is
     * unset, you need to qualify identifiers at least with the database name.
     *
     * @param databaseName The name of the database to set as the current database.
     * @see TableEnvironment#useCatalog(String)
     */
    void useDatabase(@Nullable String databaseName);

    /** Returns the table config that defines the runtime behavior of the Table API. */
    TableConfig getConfig();

    /**
     * Returns a {@link StatementSet} that accepts pipelines defined by DML statements or {@link
     * Table} objects. The planner can optimize all added statements together and then submit them
     * as one job.
     */
    StatementSet createStatementSet();

    // --- Plan compilation and restore

    /**
     * Loads a plan from a {@link PlanReference} into a {@link CompiledPlan}.
     *
     * <p>Compiled plans can be persisted and reloaded across Flink versions. They describe static
     * pipelines to ensure backwards compatibility and enable stateful streaming job upgrades. See
     * {@link CompiledPlan} and the website documentation for more information.
     *
     * <p>This method will parse the input reference and will validate the plan. The returned
     * instance can be executed via {@link CompiledPlan#execute()}.
     *
     * <p>Note: The compiled plan feature is not supported in batch mode.
     *
     * @throws TableException if the plan cannot be loaded from the filesystem, or from classpath
     *     resources, or if the plan is invalid.
     */
    @Experimental
    CompiledPlan loadPlan(PlanReference planReference) throws TableException;

    /**
     * Compiles a SQL DML statement into a {@link CompiledPlan}.
     *
     * <p>Compiled plans can be persisted and reloaded across Flink versions. They describe static
     * pipelines to ensure backwards compatibility and enable stateful streaming job upgrades. See
     * {@link CompiledPlan} and the website documentation for more information.
     *
     * <p>Note: Only {@code INSERT INTO} is supported at the moment.
     *
     * <p>Note: The compiled plan feature is not supported in batch mode.
     *
     * @see CompiledPlan#execute()
     * @see #loadPlan(PlanReference)
     * @throws TableException if the SQL statement is invalid or if the plan cannot be persisted.
     */
    @Experimental
    CompiledPlan compilePlanSql(String stmt) throws TableException;

    /**
     * Shorthand for {@code tEnv.loadPlan(planReference).execute()}.
     *
     * @see #loadPlan(PlanReference)
     * @see CompiledPlan#execute()
     */
    @Experimental
    default TableResult executePlan(PlanReference planReference) throws TableException {
        return loadPlan(planReference).execute();
    }
}
