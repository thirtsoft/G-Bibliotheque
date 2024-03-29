package com.biblio.category;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("categoryService")
@Transactional
public class CategoryServiceImpl implements ICategoryService {
	
	@Autowired
	private ICategoryDao categoryDao;

	@Override
	public List<Category> getAllCategories() {
		return categoryDao.findAll();
	}

}
