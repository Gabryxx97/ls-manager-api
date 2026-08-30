package it.lsimpresa.manager.utils.search;

import it.lsimpresa.manager.utils.DtoMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;

import java.util.Map;

public interface ISearchService {
	
	<R,T> Page<R> search(
			Map<String,Operator> filters,
			Map<String,Direction> map,
			Pageable pageable, 
			Class<T> clazz,
			DtoMapper<R,T> mapper);

}
