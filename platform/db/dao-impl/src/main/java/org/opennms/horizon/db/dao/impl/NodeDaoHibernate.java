package org.opennms.horizon.db.dao.impl;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.EntityManagerHolder;
import org.opennms.horizon.db.model.OnmsMonitoringLocation;
import org.opennms.horizon.db.model.OnmsNode;
import org.opennms.horizon.db.dao.util.AbstractDaoHibernate;

public class NodeDaoHibernate extends AbstractDaoHibernate<OnmsNode, Integer> implements NodeDao {

    public NodeDaoHibernate(EntityManagerHolder persistenceContextHolder) {
        super(persistenceContextHolder, OnmsNode.class);
    }

    @Override
    public String getLabelForId(Integer id) {
        TypedQuery<String> query = getEntityManager().createQuery("SELECT n.label FROM OnmsNode n WHERE n.id=:id", String.class);
        query.setParameter("id", id);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public String getLocationForId(Integer id) {
        TypedQuery<OnmsMonitoringLocation> query = getEntityManager().createQuery("SELECT n.location FROM OnmsNode n WHERE n.id=:id", OnmsMonitoringLocation.class);
        query.setParameter("id", id);
        try {
            return query.getSingleResult().getLocationName();
        } catch (NoResultException e) {
            return null;
        }
    }
}
