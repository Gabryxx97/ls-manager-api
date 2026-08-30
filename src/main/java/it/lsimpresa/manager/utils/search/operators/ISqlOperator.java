package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.Predicate;

public interface ISqlOperator {
	public Predicate getCriteria(String fieldName, Object... value);
}
