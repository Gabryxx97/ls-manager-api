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

public abstract class SearchControllerBase {
	
	public static final String CONTENT_RANGE_HEADER = "Content-Range";// "X-Total-Count"
	
	
	protected final Pageable pageable(int page, int size) {
		if (page<0) page = 0;
		else if (page>0) page = page-1;
		return PageRequest.of(page, size);
	}
	
	protected ResponseEntity<Void> response() {
		return ResponseEntity.ok().build();
	}
	
	protected final <T> ResponseEntity<List<T>> response(Page<T> results){
		List<T> products=results.getContent();
		   return ResponseEntity.ok()
				      .header(CONTENT_RANGE_HEADER,String.valueOf(results.getTotalElements()))
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
	
	protected final Map<String,Operator> filter(String filter){
		if (filter==null) return null;
		ObjectMapper mapper=new ObjectMapper();
		TypeReference<LinkedHashMap<String, Operator>> typeRef 
		  = new TypeReference<LinkedHashMap<String, Operator>>() {};
		  
		Map<String, Operator> filters;
		try {
			filters = mapper.readValue(filter, typeRef);
			return filters;
		} catch (Exception e) {
			throw new RuntimeException("Errore nel deserializzare i filtri.",e);
		} 
		
	}
	
	protected final Map<String,Direction> sort(String field,Direction direction){
		Map<String, Direction> sort=new HashMap<>();
		sort.put(field, direction);
		return sort;
	}
	
	protected final Map<String,Direction> sort(String sor){
		if (sor==null) return new HashMap<>();
		ObjectMapper mapper=new ObjectMapper();
		TypeReference<LinkedHashMap<String, Direction>> typeRef 
		  = new TypeReference<LinkedHashMap<String, Direction>>() {};
		  
		Map<String, Direction> sort;
		try {
			sort = mapper.readValue(sor, typeRef);
			return sort;
		} catch (Exception e) {
			throw new RuntimeException("Errore nel deserializzare i sort.",e);
		} 
	}

}
