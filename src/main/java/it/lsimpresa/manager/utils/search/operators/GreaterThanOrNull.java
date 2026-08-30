package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.OffsetDateTime;

public class GreaterThanOrNull extends SqlOperator {

	public static final String OPERATOR="GT_OR_NULL";

	public GreaterThanOrNull(CriteriaBuilder builder, Root<?> entity) {
		super(builder, entity, GreaterThanOrNull.OPERATOR);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		Path field = entity.get(fieldName);
		Predicate greaterThan;
		if (OffsetDateTime.class.equals(field.getJavaType())) {
			Path<OffsetDateTime> offsetDateTimeField = entity.get(fieldName);
			greaterThan = builder.greaterThan(offsetDateTimeField, Between.toOffsetDateTime(value[0]));
		}
		else {
			Comparable comparable=(Comparable)value[0];
			greaterThan = builder.greaterThan(field, comparable);
		}
		return builder.or(greaterThan, builder.isNull(field));
	}
}
