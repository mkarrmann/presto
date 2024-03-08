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
package com.facebook.presto.spi;

import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class TableConstraintNotFoundException
        extends NotFoundException
{
    private final Optional<String> constraintName;

    public TableConstraintNotFoundException(Optional<String> constraintName)
    {
        this(constraintName, format("Constraint '%s' not found", constraintName));
    }

    public TableConstraintNotFoundException(Optional<String> constraintName, String message)
    {
        super(message);
        this.constraintName = requireNonNull(constraintName, "constraintName is null");
    }

    public Optional<String> getConstraintName()
    {
        return constraintName;
    }
}
