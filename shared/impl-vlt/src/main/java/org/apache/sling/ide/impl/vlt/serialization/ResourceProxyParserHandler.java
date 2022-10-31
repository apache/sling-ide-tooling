/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.impl.vlt.serialization;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.vault.fs.io.DocViewParserHandler;
import org.apache.jackrabbit.vault.util.DocViewNode2;
import org.apache.jackrabbit.vault.util.DocViewProperty2;
import org.apache.sling.ide.transport.ResourceProxy;

public class ResourceProxyParserHandler implements DocViewParserHandler {

	private ResourceProxy root;
	
	private NameResolver nameResolver;

	private final Deque<ResourceProxy> resourceQueue = new LinkedList<>();

	/**
     * all type hint classes in a map (key = type integer value)
     */
    private static final Map<Integer, TypeHint> TYPE_HINT_MAP;
    
    static {
        TYPE_HINT_MAP = new HashMap<>();
        for (TypeHint hint : EnumSet.allOf(TypeHint.class)) {
            TYPE_HINT_MAP.put(hint.propertyType, hint);
        }
    }

	public ResourceProxyParserHandler() {
		
	}
	
	
	@Override
	public void setNameResolver(NameResolver nameResolver) {
		this.nameResolver = nameResolver;
	}


	@Override
	public void startDocViewNode(String nodePath, DocViewNode2 docViewNode, Optional<DocViewNode2> parentDocViewNode,
			int line, int column) throws IOException, RepositoryException {
		
		ResourceProxy currentResource = new ResourceProxy(nodePath);
		for (DocViewProperty2 property: docViewNode.getProperties()) {
			Object value = TypeHint.convertDocViewPropertyToTypedValue(property);
			if (value != null) {
				// always use qualified names (leveraging the namespace declaration from the DocView XML)
				currentResource.addProperty(nameResolver.getJCRName(property.getName()), value);
			}
		}
		if (root == null) {
			root = currentResource;
		} else {
			ResourceProxy parent = resourceQueue.peekLast();
            parent.addChild(currentResource);
		}
		resourceQueue.add(currentResource); 
		
	}

	@Override
	public void endDocViewNode(String nodePath, DocViewNode2 docViewNode, Optional<DocViewNode2> parentDocViewNode,
			int line, int column) throws IOException, RepositoryException {
		resourceQueue.removeLast();
	}

	/**
     * Each enum implements the {@link TypeHint#parseValues(String[], boolean)} in a way, that the String[] value is converted to the closest underlying type.
     */
    static enum TypeHint {
        UNDEFINED(PropertyType.UNDEFINED) {
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return STRING.parseValues(values, explicitMultiValue);
            }
        },
        STRING(PropertyType.STRING) {
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return values[0];
                } else {
                    return values;
                }
            }
        },
        BINARY(PropertyType.BINARY) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return null;
            }
        },
        BOOLEAN(PropertyType.BOOLEAN) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return Boolean.valueOf(values[0]);
                }

                Boolean[] ret = new Boolean[values.length];
                for (int i = 0; i < values.length; i++) {
                    ret[i] = Boolean.parseBoolean(values[i]);
                }
                return ret;
            }
        },
        DATE(PropertyType.DATE) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {

                if (values.length == 1 && !explicitMultiValue) {
                    return ISO8601.parse(values[0]);
                }

                Calendar[] ret = new Calendar[values.length];
                for (int i = 0; i < values.length; i++) {
                    ret[i] = ISO8601.parse(values[i]);
                }
                return ret;
            }
        },
        DOUBLE(PropertyType.DOUBLE) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return Double.parseDouble(values[0]);
                }

                Double[] ret = new Double[values.length];
                for (int i = 0; i < values.length; i++) {
                    ret[i] = Double.parseDouble(values[i]);
                }
                return ret;
            }
        },
        LONG(PropertyType.LONG) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return Long.valueOf(values[0]);
                }
                
                Long[] ret = new Long[values.length];
                for ( int i =0 ; i < values.length; i++ ) {
                    ret[i] = Long.valueOf(values[i]);
                }
                return ret;
            }
        },
        DECIMAL(PropertyType.DECIMAL) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return new BigDecimal(values[0]);
                }
                
                BigDecimal[] ret = new BigDecimal[values.length];
                for ( int i = 0; i < values.length; i++) {
                    ret[i] = new BigDecimal(values[i]);
                }
                return ret;
            }
        },
        NAME(PropertyType.NAME) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return values[0];
                }
                return values;
            }
        },
        PATH(PropertyType.PATH) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return NAME.parseValues(values, explicitMultiValue);
            }
        },
        REFERENCE(PropertyType.REFERENCE) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                if (values.length == 1 && !explicitMultiValue) {
                    return UUID.fromString(values[0]);
                }

                UUID[] refs = new UUID[values.length];
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    refs[i] = UUID.fromString(value);
                }

                return refs;
            }

        },
        WEAKREFERENCE(PropertyType.WEAKREFERENCE) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                return REFERENCE.parseValues(values, explicitMultiValue);
            }
        },
        URI(PropertyType.URI) {
            @Override
            Object parseValues(String[] values, boolean explicitMultiValue) {
                try {
                    if (values.length == 1 && !explicitMultiValue) {
                        return new java.net.URI(values[0]);
                    }
    
                    java.net.URI[] refs = new java.net.URI[values.length];
                    for (int i = 0; i < values.length; i++) {
                        String value = values[i];
                        refs[i] = new java.net.URI(value);
                    }
                    return refs;
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException("Given value cannot be converted to URI", e);
                }
            }
        };

        static Object convertDocViewPropertyToTypedValue(DocViewProperty2 property) {
            TypeHint hint = TYPE_HINT_MAP.get(property.getType());
            if (hint == null) {
                throw new IllegalArgumentException("Unknown type value '" + property.getType() + "'");
            }
            return hint.parseValues(property.getStringValues().toArray(new String[0]), property.isMultiValue());
        }

        private final int propertyType;

        /**
         * 
         * @param propertyType one of type values being defined in {@link javax.jcr.PropertyType}
         */
        private TypeHint(int propertyType) {

            this.propertyType = propertyType;
        }

        abstract Object parseValues(String[] values, boolean explicitMultiValue);

    }

	public ResourceProxy getRoot() {
		return root;
	}
}
