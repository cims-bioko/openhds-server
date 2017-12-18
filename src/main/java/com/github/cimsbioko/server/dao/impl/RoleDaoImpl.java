package com.github.cimsbioko.server.dao.impl;

import java.util.List;

import com.github.cimsbioko.server.dao.RoleDao;
import com.github.cimsbioko.server.domain.model.Role;
import com.github.cimsbioko.server.domain.model.User;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

/**
 * Specialized class for Role entity
 * Had to override the findPaged method as it was using an outer join  because
 * the privileges property is eagerly loaded. It would not get the correct
 * Role entities for a page.
 *
 * @author Dave Roberge
 */
@Repository("roleDao")
public class RoleDaoImpl extends BaseDaoImpl<Role, String> implements RoleDao {

    public RoleDaoImpl() {
        super(Role.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Role> findPaged(int maxResults, int firstResult) {
        Criteria criteria = getSession().createCriteria(entityType);
        addImplicitRestrictions(criteria);

        return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).
                setFirstResult(firstResult).setMaxResults(maxResults).
                // this overrides the eager fetch mode to use a separate select statement.
                // refer to: http://docs.jboss.org/hibernate/core/3.5/api/org/hibernate/FetchMode.html
                        setFetchMode("privileges", FetchMode.SELECT).
                        list();
    }

    @SuppressWarnings("unchecked")
    public List<User> findAllUsersWithRole(Role role) {
        Criteria criteria = getSession().createCriteria(User.class)
                .createAlias("roles", "theRoles")
                .add(Restrictions.eq("theRoles.uuid", role.getUuid()))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return (List<User>) criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<Role> findAllRolesExcept(Role role) {
        Criteria criteria = getSession().createCriteria(entityType);
        addImplicitRestrictions(criteria);
        return (List<Role>) criteria.add(Restrictions.ne("uuid", role.getUuid()))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .list();
    }
}
