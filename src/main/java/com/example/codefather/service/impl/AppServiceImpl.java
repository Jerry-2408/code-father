package com.example.codefather.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.example.codefather.model.entity.App;
import com.example.codefather.mapper.AppMapper;
import com.example.codefather.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/Jerry-2408">Jerry</a>
 * @since 2025-12-25
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
