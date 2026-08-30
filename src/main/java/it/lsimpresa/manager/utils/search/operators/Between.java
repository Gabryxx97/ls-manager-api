package it.lsimpresa.manager.utils.search.operators;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.OffsetDateTime;
import java.util.Date;


public class Between extends SqlOperator{
	
	public static final String OPERATOR="BETWEEN";
	
	public Between(CriteriaBuilder builder, Root<?> entity) {
		super(builder, entity, Between.OPERATOR);
	}
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Predicate getCriteria(String fieldName, Object... value) {
		if(value==null || value.length!=2) throw new RuntimeException("Numero di parametri non valido per l' operatore: "+OPERATOR);

		Path field = entity.get(fieldName);
		Class<?> fieldType = field.getJavaType();
		Predicate res=null;
		if (OffsetDateTime.class.equals(fieldType)) {
			OffsetDateTime val0 = toOffsetDateTime(value[0]);
			OffsetDateTime val1 = toOffsetDateTime(value[1]);
			Path<OffsetDateTime> offsetDateTimeField = entity.get(fieldName);
			res=builder.between(offsetDateTimeField, val0, val1);
		}
		else if (Date.class.equals(fieldType) || fieldName.toLowerCase().contains("data") ||
			fieldName.toLowerCase().contains("timestamp") ||
			fieldName.toLowerCase().contains("date")) {
			long val0=((Long)value[0]).longValue();
			long val1=((Long)value[1]).longValue();
			Path<Date> dateField = entity.get(fieldName);
			res=builder.between(dateField,new Date(val0) ,new Date(val1));
		}
		else {
			Comparable val0 = (Comparable) value[0];
			Comparable val1 = (Comparable) value[1];
			res=builder.between(field,val0, val1);
		}
		return res;
	}

	static OffsetDateTime toOffsetDateTime(Object value) {
		if (value instanceof OffsetDateTime offsetDateTime) {
			return offsetDateTime;
		}
		if (value instanceof String text) {
			return OffsetDateTime.parse(text);
		}
		throw new IllegalArgumentException("Valore non valido per OffsetDateTime: " + value);
	}

	@Override
	public String getOperator() {
		return Between.OPERATOR;
	}

}
