package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class Equal extends SqlOperator{
	
	public static final String OPERATOR="EQUAL";
	
	public Equal(CriteriaBuilder builder,Root<?> entity) {
		super(builder, entity,Equal.OPERATOR);
	}
	
	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		if (fieldName.contains("#")) {
			String[] splitted=fieldName.split("\\#");
			if (splitted.length>2 ) throw new RuntimeException("E' permesso solo un sottolivello.");
			String first=splitted[0];
			String second=splitted[1];
			if (!second.equals("id")) throw new RuntimeException("E' permesso solo il campo id del sottolivello.");
			return builder.equal(entity.get(first).<Long>get(second), value[0]);
		}
		else return builder.equal(entity.get(fieldName), value[0]);
	}

}
