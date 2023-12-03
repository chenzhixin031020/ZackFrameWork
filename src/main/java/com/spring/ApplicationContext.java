package com.spring;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {

    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap();
    private ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap();
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();
    public ApplicationContext(Class configClass){
        //扫描类
        List<Class> classList = scan(configClass);

        for (Class aClass : classList) {
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setBeanClass(aClass);


            if (aClass.isAnnotationPresent(Component.class)){
                Component component = (Component) aClass.getAnnotation(Component.class);
                String beanName = component.value();

                if(aClass.isAnnotationPresent(Scope.class)) {
                    Scope scope = (Scope) aClass.getAnnotation(Scope.class);
                    beanDefinition.setScope(scope.value());

                }else {
                    beanDefinition.setScope("singleton");
                }

                if (BeanPostProcessor.class.isAssignableFrom(aClass)){
                    try {
                        BeanPostProcessor bpp = (BeanPostProcessor) aClass.getDeclaredConstructor().newInstance();
                        beanPostProcessorList.add(bpp);

                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }


                beanDefinitionMap.put(beanName, beanDefinition);

            }
        }



        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

            if (beanDefinition.getScope().equals("singleton")){
                    //生成bean
                    Object bean = createBean(beanName,beanDefinition);
                    singletonObjects.put(beanName,bean);

                }

        }

    }

    private Object createBean(String beanName,BeanDefinition beanDefinition) {
        Class beanClass = beanDefinition.getBeanClass();
        try {
            //实例化
            Object bean = beanClass.getDeclaredConstructor().newInstance();


            //填充属性
            Field[] fields = beanClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)){
                    Object object = getBean(field.getName());
                    field.setAccessible(true);
                    field.set(bean,object);
                }
            }

            //Aware
            if (bean instanceof BeanNameAware){
                ((BeanNameAware) bean).setBeanName(beanName);
            }


            //....程序员定义的逻辑
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                beanPostProcessor.postProcessBeforeInitialization(bean,beanName);
            }

            //初始化
            if (bean instanceof InitializingBean){
                ((InitializingBean) bean).afterPropertiesSet();
            }


            //....程序员定义的逻辑 aop
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                beanPostProcessor.postProcessAfterInitialization(bean,beanName);
            }

            return bean;


        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

    }


    private List<Class> scan(Class configClass) {
        List<Class> classList = new ArrayList<>();

        ComponentScan componentScan = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
        String scanPath = componentScan.value();
        scanPath = scanPath.replace(".","/");
        //System.out.println(scanPath);

        ClassLoader classLoader = ApplicationContext.class.getClassLoader();
        URL resource = classLoader.getResource(scanPath);

        File file = new File(resource.getFile());    //目录对象
        File[] files = file.listFiles();
        for (File f : files) {

            //System.out.println(f);

            String absolutePath = f.getAbsolutePath();
            absolutePath = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
            absolutePath = absolutePath.replace("\\",".");
            //System.out.println(absolutePath);

            try{
                Class<?> aClass = classLoader.loadClass(absolutePath);

                classList.add(aClass);

            }catch (ClassNotFoundException e){
                e.printStackTrace();
            }

        }
        return classList;
    }

    public Object getBean(String beanName){
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition.getScope().equals("prototype")){
                return createBean(beanName,beanDefinition);
        }else {
            Object bean = singletonObjects.get(beanName);
            if (bean == null){
                Object o = createBean(beanName,beanDefinition);
                singletonObjects.put(beanName,o);

                return o;
            }
            return bean;
        }
    }
}
