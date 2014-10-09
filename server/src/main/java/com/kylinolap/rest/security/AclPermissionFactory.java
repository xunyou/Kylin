/*
 * Copyright 2013-2014 eBay Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kylinolap.rest.security;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.acls.domain.DefaultPermissionFactory;
import org.springframework.security.acls.model.Permission;

/**
 * @author xduo
 *
 */
public class AclPermissionFactory extends DefaultPermissionFactory {

    public AclPermissionFactory() {
        super();
        registerPublicPermissions(AclPermission.class);
    }

    public static List<Permission> getPermissions() {
        List<Permission> permissions = new ArrayList<Permission>();
        Field[] fields = AclPermission.class.getFields();

        for (Field field : fields) {
            try {
                Object fieldValue = field.get(null);

                if (Permission.class.isAssignableFrom(fieldValue.getClass())) {
                    // Found a Permission static field
                    permissions.add((Permission) fieldValue);
                }
            } catch (Exception ignore) {
            }
        }

        return permissions;
    }

    public static Permission getPermission(String perName) {
        Field[] fields = AclPermission.class.getFields();

        for (Field field : fields) {
            try {
                Object fieldValue = field.get(null);

                if (Permission.class.isAssignableFrom(fieldValue.getClass())) {
                    // Found a Permission static field
                    if (perName.equals(field.getName())) {
                        return (Permission) fieldValue;
                    }
                }
            } catch (Exception ignore) {
            }
        }

        return null;
    }
}