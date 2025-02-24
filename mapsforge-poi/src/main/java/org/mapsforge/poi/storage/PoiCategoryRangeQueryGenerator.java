/*
 * Copyright 2010, 2011 mapsforge.org
 * Copyright 2010, 2011 Karsten Groll
 * Copyright 2015-2017 devemux86
 * Copyright 2017 Gustl22
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.poi.storage;

import org.mapsforge.core.model.LatLong;

import java.util.Collection;
import java.util.Iterator;

/**
 * This class generates a prepared SQL query for retrieving POIs filtered by a given
 * {@link PoiCategoryFilter}.
 */
public final class PoiCategoryRangeQueryGenerator {
    private PoiCategoryRangeQueryGenerator() {
        // no-op, for privacy
    }

    /**
     * Gets the SQL query that looks up POI entries.
     *
     * @param filter  The filter object for determining all wanted categories.
     * @param count   Count of patterns to search in points of interest names (may be 0).
     * @param orderBy {@link LatLong} location of the sort.
     * @return The SQL query.
     */
    public static String getSQLSelectString(PoiCategoryFilter filter, int count, LatLong orderBy) {
        StringBuilder sb = new StringBuilder();
        sb.append(DbConstants.FIND_IN_BOX_CLAUSE_SELECT);
        sb.append(DbConstants.JOIN_CATEGORY_CLAUSE);
        if (count > 0) {
            sb.append(DbConstants.JOIN_DATA_CLAUSE);
        }
        sb.append(DbConstants.FIND_IN_BOX_CLAUSE_WHERE);
        sb.append(getSQLWhereClauseString(filter));
        for (int i = 0; i < count; i++) {
            sb.append(DbConstants.FIND_BY_DATA_CLAUSE);
        }
        if (orderBy != null) {
            sb.append(" ORDER BY ((").append(orderBy.latitude).append(" - poi_index.lat) * (").append(orderBy.latitude).append(" - poi_index.lat))")
                    .append(" + ((").append(orderBy.longitude).append(" - poi_index.lon) * (").append(orderBy.longitude).append(" - poi_index.lon)) ASC");
        }
        return (sb.append(" LIMIT ?;").toString());
    }

    /**
     * Gets the WHERE clause for the SQL query that looks up POI entries.
     *
     * @param filter The filter object for determining all wanted categories.
     * @return The WHERE clause.
     */
    private static String getSQLWhereClauseString(PoiCategoryFilter filter) {
        Collection<PoiCategory> superCategories = filter.getAcceptedSuperCategories();

        if (superCategories.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(" AND (");
        // for each super category
        for (Iterator<PoiCategory> superCatIter = superCategories.iterator(); superCatIter.hasNext(); ) {
            PoiCategory superCat = superCatIter.next();

            // All child categories of the super category, including their children
            Collection<PoiCategory> categories = superCat.deepChildren();
            // Don't forget the super category itself in the search!
            categories.add(superCat);

            sb.append(DbConstants.FIND_IN_BOX_CLAUSE_WHERE_CATEGORY_IN);
            // for each category
            for (Iterator<PoiCategory> catIter = categories.iterator(); catIter.hasNext(); ) {
                PoiCategory cat = catIter.next();
                sb.append(cat.getID());
                if (catIter.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(")");

            // append OR if it is not the last
            if (superCatIter.hasNext()) {
                sb.append(" OR ");
            }
        }
        sb.append(")");

        return sb.toString();
    }
}
