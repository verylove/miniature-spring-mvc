package com.gupaoedu.vip.mvc.demo.mvc.action;

import com.gupaoedu.vip.mvc.demo.service.INamedService;
import com.gupaoedu.vip.mvc.demo.service.IService;
import com.gupaoedu.vip.mvc.framework.annotation.*;
import com.gupaoedu.vip.mvc.framework.servlet.GPModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO
 *
 * @author lrj
 * @see
 * @since 2018.01.17
 */
@GPController
@GPRequestMapping("/web")
public class FirstAction {
    @GPAutowired
    private IService service;
    
    @GPAutowired("myName")
    private INamedService namedService;
    // 原来的/query.json
    @GPRequestMapping("/query/.*.json")//这里可以写成正则的,给/query/{add}.json奠定了基础
    //@GPResponseBody
    public GPModelAndView query(HttpServletRequest request, HttpServletResponse response, @GPRequestParam("name") String name,
                                @GPRequestParam("addr") String addr){
        //out(response,"get params name = " + name);
        Map<String,Object> model = new HashMap<>();
        model.put("name",name);
        model.put("addr",addr);
        return new GPModelAndView("first.gpml",model);
    }

    @GPRequestMapping("/add.json")
    public GPModelAndView add(HttpServletRequest request, HttpServletResponse response){
        out(response,"this is json string");
        return null;
    }
    
    public void  out( HttpServletResponse response,String str){
        try {
            response.getWriter().write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
