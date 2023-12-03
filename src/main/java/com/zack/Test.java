package com.zack;

import com.spring.ApplicationContext;
import com.zack.service.OrderService;


public class Test {


    public static void main(String[] args){
        //启动Spring
        ApplicationContext applicationContext =new ApplicationContext(AppConfig.class);

        //getBean

        System.out.println(applicationContext.getBean("orderService"));




    }


}
