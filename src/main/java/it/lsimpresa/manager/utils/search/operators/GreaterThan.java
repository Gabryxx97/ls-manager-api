package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.OffsetDateTime;

public class GreaterThan extends SqlOperator {
	
	public static final String OPERATOR="GT";

	public GreaterThan(CriteriaBuilder builder,Root<?> entity) {
		super(builder, entity,GreaterThan.OPERATOR);
	}
		
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		Path field = entity.get(fieldName);
		if (OffsetDateTime.class.equals(field.getJavaType())) {
			Path<OffsetDateTime> offsetDateTimeField = entity.get(fieldName);
			return builder.greaterThan(offsetDateTimeField, Between.toOffsetDateTime(value[0]));
		}
		Comparable comparable=(Comparable)value[0];
		return builder.greaterThan(field, comparable);
	}

}
