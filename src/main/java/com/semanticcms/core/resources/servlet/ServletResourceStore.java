/*
 * semanticcms-core-resources-servlet - Redistributable sets of SemanticCMS resources produced by the local servlet container.
 * Copyright (C) 2017  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-resources-servlet.
 *
 * semanticcms-core-resources-servlet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-resources-servlet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-resources-servlet.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.resources.servlet;

import com.aoindustries.servlet.ServletContextCache;
import com.aoindustries.util.Tuple2;
import com.semanticcms.core.resources.Resource;
import com.semanticcms.core.resources.ResourceStore;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;

/**
 * Accesses resources in the local {@link ServletContext}.
 * <p>
 * Optionally, and by default, uses {@link ServletContextCache} to work around some performance issues
 * with direct use of {@link ServletContext}, especially when a large number of JAR files are deployed
 * to <code>/WEB-INF/lib</code>, which is one of the shared content distribution models for
 * <a href="https://semanticcms.com/">SemanticCMS</a>.
 * </p>
 *
 * @see  ServletContextCache
 * @see  ServletContext#getResource(java.lang.String)
 * @see  ServletContext#getResourceAsStream(java.lang.String)
 * @see  ServletContext#getRealPath(java.lang.String)
 */
public class ServletResourceStore implements ResourceStore {

	private static final String INSTANCES_SERVLET_CONTEXT_KEY = ServletResourceStore.class.getName() + ".instances";

	/**
	 * Gets the servlet store for the given context and prefix.
	 * Only one {@link ServletResourceStore} is created per unique context and prefix.
	 *
	 * @param  prefix  Must be either empty or a {@link Resource#checkPath(java.lang.String) valid path}.
	 *                 Any trailing slash "/" will be stripped, after validity check.
	 *
	 * @param cached  Enables use of {@link ServletContextCache} to workaround some performance issues with direct use of {@link ServletContext},
	 *                but introduces a potential delay of up to {@link ServletContextCache#REFRESH_INTERVAL} milliseconds (current 5 seconds)
	 *                before new or moved content becomes visible.
	 */
	public static ServletResourceStore getInstance(ServletContext servletContext, String prefix, boolean cached) {
		if(!prefix.isEmpty()) {
			Resource.checkPath(prefix);
			// Strip any trailing slash
			if(prefix.endsWith("/")) prefix = prefix.substring(0, prefix.length() - 1);
			assert !prefix.endsWith("/") : "Trailing double-slash should have been caught by Resource.checkPath";
		}

		Map<Tuple2<String,Boolean>,ServletResourceStore> instances;
		synchronized(servletContext) {
			@SuppressWarnings("unchecked")
			Map<Tuple2<String,Boolean>,ServletResourceStore> map = (Map<Tuple2<String,Boolean>,ServletResourceStore>)servletContext.getAttribute(INSTANCES_SERVLET_CONTEXT_KEY);
			if(map == null) {
				map = new HashMap<Tuple2<String,Boolean>,ServletResourceStore>();
				servletContext.setAttribute(INSTANCES_SERVLET_CONTEXT_KEY, map);
			}
			instances = map;
		}
		synchronized(instances) {
			Tuple2<String,Boolean> key = new Tuple2<String,Boolean>(prefix, cached);
			ServletResourceStore store = instances.get(key);
			if(store == null) {
				store = new ServletResourceStore(servletContext, prefix, cached);
				instances.put(key, store);
			}
			return store;
		}
	}

	/**
	 * Gets a cached instance.
	 *
	 * @see  #getInstance(javax.servlet.ServletContext, java.lang.String, boolean)
	 */
	public static ServletResourceStore getInstance(ServletContext servletContext, String prefix) {
		return getInstance(servletContext, prefix, true);
	}

	final ServletContext servletContext;
	final String prefix;
	final ServletContextCache cache;

	private ServletResourceStore(ServletContext servletContext, String prefix, boolean cached) {
		this.servletContext = servletContext;
		this.prefix = prefix;
		this.cache = cached ? ServletContextCache.getCache(servletContext) : null;
	}

	public ServletContext getServletContext() {
		return servletContext;
	}

	public String getPrefix() {
		return prefix;
	}

	@Override
	public String toString() {
		return "servlet:" + prefix;
	}

	@Override
	public ServletResource getResource(String path) {
		return new ServletResource(this, path);
	}
}
