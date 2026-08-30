package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Root;
import lombok.Getter;

public abstract class SqlOperator implements ISqlOperator{

	protected CriteriaBuilder builder;
	protected Root<?> entity;
	
	@Getter 
	private final String operator;
	
	protected SqlOperator(CriteriaBuilder builder,Root<?> entity,String operator) {
		this.builder=builder;
		this.entity = entity;
		this.operator=operator;
	}
	
	
	
}
