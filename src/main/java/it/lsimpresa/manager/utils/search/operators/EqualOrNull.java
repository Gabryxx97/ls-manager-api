package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class EqualOrNull extends SqlOperator {

    public static final String OPERATOR = "EQUAL_OR_NULL";

    public EqualOrNull(CriteriaBuilder builder, Root<?> entity) {
        super(builder, entity, OPERATOR);
    }

    @Override
    public Predicate getCriteria(String fieldName, Object... value) {
        Path<?> field = entity.get(fieldName);
        return builder.or(builder.equal(field, value[0]), builder.isNull(field));
    }
}
