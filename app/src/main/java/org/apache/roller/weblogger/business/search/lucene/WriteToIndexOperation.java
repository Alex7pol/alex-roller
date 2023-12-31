/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
/* Created on Aug 12, 2003 */
package org.apache.roller.weblogger.business.search.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An operation that writes to index.
 * @author Mindaugas Idzelis (min@idzelis.com)
 */
public abstract class WriteToIndexOperation extends IndexOperation {
    
    public WriteToIndexOperation(LuceneIndexManager mgr) {
        super(mgr);
    }
    
    private static Log logger =
            LogFactory.getFactory().getInstance(WriteToIndexOperation.class);
    
    @Override
    public void run() {
        try {
            manager.getReadWriteLock().writeLock().lock();
            logger.debug("Starting search index operation");
            doRun();
            logger.debug("Search index operation complete");

        } catch (Exception e) {
            logger.error("Error acquiring write lock on index", e);
            
        } finally {
            manager.getReadWriteLock().writeLock().unlock();
        }
        manager.resetSharedReader();
    }
}
