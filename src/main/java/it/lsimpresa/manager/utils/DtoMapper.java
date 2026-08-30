package it.lsimpresa.manager.utils;

import java.util.List;

public interface DtoMapper<R,T> {
	List<R> map(List<T> users);
}
