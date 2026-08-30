package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;


public class In extends  SqlOperator{
	
	public static final String OPERATOR="IN";
	
	public In(CriteriaBuilder builder, Root<?> entity) {
		super(builder, entity, In.OPERATOR);
	}

	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		Expression<?> parentExpression = entity.get(fieldName);
		return parentExpression.in(value);
	}

}
