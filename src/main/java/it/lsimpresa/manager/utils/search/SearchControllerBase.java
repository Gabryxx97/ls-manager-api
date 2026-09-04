package it.lsimpresa.manager.utils.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class SearchControllerBase {
	
	public static final String CONTENT_RANGE_HEADER = "Content-Range";// "X-Total-Count"
	private static final Set<String> SINGLE_VALUE_OPERATORS = Set.of(
			"EQUAL", "EQUAL_OR_NULL", "LIKE", "ILIKE", "GT", "GT_OR_NULL",
			"GTE", "LT", "LTE");
	private static final Set<String> TWO_VALUE_OPERATORS = Set.of("BETWEEN");
	private static final Set<String> MULTI_VALUE_OPERATORS = Set.of("IN");
	
	
	protected final Pageable pageable(int page, int size) {
		if (page < 1) {
			throw new IllegalArgumentException("La pagina deve essere maggiore o uguale a 1");
		}
		if (size < 1 || size > 100) {
			throw new IllegalArgumentException("La dimensione pagina deve essere compresa tra 1 e 100");
		}
		return PageRequest.of(page - 1, size);
	}
	
	protected ResponseEntity<Void> response() {
		return ResponseEntity.ok().build();
	}
	
	protected final <T> ResponseEntity<List<T>> response(Page<T> results){
		List<T> products=results.getContent();
		long first = results.getPageable().getOffset();
		long last = products.isEmpty() ? first : first + products.size() - 1;
		   return ResponseEntity.ok()
				      .header(CONTENT_RANGE_HEADER,
							  "items " + first + "-" + last + "/" + results.getTotalElements())
				      .body(products);
	}
	
	protected final <T> ResponseEntity<List<T>> response(List<T> results){
		   return ResponseEntity.ok()
				      .header(CONTENT_RANGE_HEADER,String.valueOf(results.size()))
				      .body(results);
	}
	
	protected final <T> ResponseEntity<T> response(T result){
		  return ResponseEntity.ok()
				     .body(result);
	}
	
	protected final Map<String,Operator> filter(
			String filter,
			Map<String, Set<String>> allowedFilters) {
		if (filter == null || filter.isBlank()) return Map.of();
		ObjectMapper mapper=new ObjectMapper();
		TypeReference<LinkedHashMap<String, Operator>> typeRef 
		  = new TypeReference<LinkedHashMap<String, Operator>>() {};
		  
		Map<String, Operator> filters;
		try {
			filters = mapper.readValue(filter, typeRef);
			for (Map.Entry<String, Operator> entry : filters.entrySet()) {
				Set<String> allowedOperators = allowedFilters.get(entry.getKey());
				if (allowedOperators == null) {
					throw new IllegalArgumentException("Campo filtro non consentito: " + entry.getKey());
				}
				validateOperator(entry.getKey(), entry.getValue(), allowedOperators);
			}
			return filters;
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Formato filtri non valido", e);
		} 
		
	}
	
	protected final Map<String,Direction> sort(String field,Direction direction){
		Map<String, Direction> sort=new HashMap<>();
		sort.put(field, direction);
		return sort;
	}
	
	protected final Map<String,Direction> sort(String sor, Set<String> allowedFields){
		if (sor == null || sor.isBlank()) return new HashMap<>();
		ObjectMapper mapper=new ObjectMapper();
		TypeReference<LinkedHashMap<String, Direction>> typeRef 
		  = new TypeReference<LinkedHashMap<String, Direction>>() {};
		  
		Map<String, Direction> sort;
		try {
			sort = mapper.readValue(sor, typeRef);
			if (sort.size() > 1) {
				throw new IllegalArgumentException("È consentito un solo criterio di ordinamento");
			}
			for (String field : sort.keySet()) {
				if (!allowedFields.contains(field)) {
					throw new IllegalArgumentException("Campo ordinamento non consentito: " + field);
				}
			}
			return sort;
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("Formato ordinamento non valido", e);
		} 
	}

	private void validateOperator(String field, Operator operator, Set<String> allowedOperators) {
		if (operator == null || operator.getOperator() == null) {
			throw new IllegalArgumentException("Operatore mancante per il campo: " + field);
		}
		String name = operator.getOperator().toUpperCase();
		if (!allowedOperators.contains(name)) {
			throw new IllegalArgumentException("Operatore non consentito per il campo: " + field);
		}
		operator.setOperator(name);
		Object[] values = operator.getValues();
		if (values == null || values.length == 0) {
			throw new IllegalArgumentException("Valore filtro mancante per il campo: " + field);
		}
		if (SINGLE_VALUE_OPERATORS.contains(name) && values.length != 1) {
			throw new IllegalArgumentException("Il filtro richiede un solo valore: " + field);
		}
		if (TWO_VALUE_OPERATORS.contains(name) && values.length != 2) {
			throw new IllegalArgumentException("Il filtro richiede due valori: " + field);
		}
		if (!SINGLE_VALUE_OPERATORS.contains(name)
				&& !TWO_VALUE_OPERATORS.contains(name)
				&& !MULTI_VALUE_OPERATORS.contains(name)) {
			throw new IllegalArgumentException("Operatore non supportato: " + name);
		}
	}

}
