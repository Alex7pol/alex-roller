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
/* Created on Jul 16, 2003 */
package org.apache.roller.weblogger.business.search.lucene;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.Weblogger;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;

/**
 * An index operation that rebuilds a given users index (or all indexes).
 * 
 * @author Mindaugas Idzelis (min@idzelis.com)
 */
public class RebuildWebsiteIndexOperation extends WriteToIndexOperation {

    // ~ Static fields/initializers
    // =============================================

    private static Log logger = LogFactory.getFactory().getInstance(
            RebuildWebsiteIndexOperation.class);

    // ~ Instance fields
    // ========================================================

    private Weblog website;
    private Weblogger roller;

    // ~ Constructors
    // ===========================================================

    /**
     * Create a new operation that will recreate an index.
     * 
     * @param website
     *            The website to rebuild the index for, or null for all users.
     */
    public RebuildWebsiteIndexOperation(Weblogger roller, LuceneIndexManager mgr,
            Weblog website) {
        super(mgr);
        this.roller = roller;
        this.website = website;
    }

    // ~ Methods
    // ================================================================

    @Override
    public void doRun() {

        Date start = new Date();

        // since this operation can be run on a separate thread we must treat
        // the weblog object passed in as a detached object which is proned to
        // lazy initialization problems, so requery for the object now
        if (this.website != null) {
            logger.debug("Reindexining weblog " + website.getHandle());
            try {
                this.website = roller.getWeblogManager().getWeblog(
                        this.website.getId());
            } catch (WebloggerException ex) {
                logger.error("Error getting website object", ex);
                return;
            }
        } else {
            logger.debug("Reindexining entire site");
        }

        IndexWriter writer = beginWriting();

        try {
            if (writer != null) {

                // Delete Doc
                Term tWebsite = null;
                if (website != null) {
                    tWebsite = IndexUtil.getTerm(FieldConstants.WEBSITE_HANDLE,
                            website.getHandle());
                }
                if (tWebsite != null) {
                    writer.deleteDocuments(tWebsite);
                } else {
                    Term all = IndexUtil.getTerm(FieldConstants.CONSTANT,
                            FieldConstants.CONSTANT_V);
                    writer.deleteDocuments(all);
                }

                // Add Doc
                WeblogEntryManager weblogManager = roller
                        .getWeblogEntryManager();
                WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
                wesc.setWeblog(website);
                wesc.setStatus(PubStatus.PUBLISHED);
                List<WeblogEntry> entries = weblogManager.getWeblogEntries(wesc);

                logger.debug("Entries to index: " + entries.size());

                for (WeblogEntry entry : entries) {
                    writer.addDocument(getDocument(entry));
                    logger.debug(MessageFormat.format(
                            "Indexed entry {0}: {1}",
                            entry.getPubTime(), entry.getAnchor()));
                }

                // release the database connection
                roller.release();
            }
        } catch (Exception e) {
            logger.error("ERROR adding/deleting doc to index", e);
        } finally {
            endWriting();
            if (roller != null) {
                roller.release();
            }
        }

        Date end = new Date();
        double length = (end.getTime() - start.getTime()) / (double) RollerConstants.SEC_IN_MS;

        if (website == null) {
            logger.info("Completed rebuilding index for all users in '"
                    + length + "' secs");
        } else {
            logger.info("Completed rebuilding index for website handle: '"
                    + website.getHandle() + "' in '" + length + "' seconds");
        }
    }
}
