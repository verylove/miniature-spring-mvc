package com.gupaoedu.vip.mvc.framework.context;

import com.gupaoedu.vip.mvc.framework.annotation.GPAutowired;
import com.gupaoedu.vip.mvc.framework.annotation.GPController;
import com.gupaoedu.vip.mvc.framework.annotation.GPService;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO
 *
 * @author lrj
 * @see
 * @since 2018.01.17
 */
public class GPAppliactionContext {
    
    //IOC容器就是个map
    private Map<String,Object> instanceMapping = new ConcurrentHashMap<>();
    
    //所以要生成一个缓存
    private List<String> classCache = new ArrayList<>();

    Properties config = new Properties();
    
    //类似于内部的配置信息，我们在外面是看不到的
    //我们能够看到的只有ioc容器，通过getBean的方法来间接调用的
    public GPAppliactionContext(String location){
        InputStream is = null;
        try {
            //1、定位：直接把这个location从我们的类路径下找到，这个location在我们的web.xml已经配置好了
            is = this.getClass().getClassLoader().getResourceAsStream(location);
            //2、加载
            config.load(is);
            //3、注册（这里省了，没必要再去封装一个BeanDefinition）把所有class找出来存着
            String packageName = config.getProperty("scanPackage"); //拿到包名
            doRegister(packageName);
            //4、初始化，只要循环class（非常有技术含量）
            doCreateBean();
            //5、注入
            populate();
            
            //先加载配置文件
            //定位、加载、注册、初始化、注入
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("IOC 容器已经初始化");
    }

    //注册（把符合条件的所有的class全部找出来注册到我们的缓存里面去）
    private void doRegister(String packageName){
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/")); //要把packageNam变成一个文件夹路径，去这个文件夹里面找
        File dir = new File(url.getFile());//把这个文件夹的文件全部读出来
        for (File file: dir.listFiles()) {
            if(file.isDirectory()){ //如果是个文件夹就递归
                doRegister(packageName + "." + file.getName());
            }else{ //如果不是文件夹就加到缓存里面，就相当于是注册嘛
                classCache.add(packageName + "." + file.getName().replace(".class","").trim());
            }
            
        }
        
    }
    
    //初始化
    private void doCreateBean(){
        //检查看有没有注册信息，注册信息里面保存了所有的class名字
        //BeanDefinition 保存了类的名字，也保存了类和类之间的关系(Map/List/Set/Ref/Parent)
        if(classCache.size() == 0 ){ //如果没有就不玩了
            return;
        }
        try{
            for (String className : classCache) {
                
                //我们这里不搞这么复杂，就用反射。这里spring是用了代理，还判断是用jdk还是用cglib
                Class<?> clazz = Class.forName(className);
                
                //哪个类需要初始化，哪个类不要初始化，只要加了@Service @Controller都要初始化
                if(clazz.isAnnotationPresent(GPController.class)){
                    //初始化名字起啥？默认就是类名首字母小写
                    String id = lowerFirstChar(clazz.getSimpleName());
                    instanceMapping.put(id,clazz.newInstance());
                }else if(clazz.isAnnotationPresent(GPService.class)){
                    GPService service = clazz.getAnnotation(GPService.class);
                    //如果设置了自定义名字，就优先用他自己定义的名字
                    String id = service.value();
                    if(!"".equals(id.trim())){
                        instanceMapping.put(id,clazz.newInstance());
                        continue;
                    }
                    
                    //如果是空的，就用默认规则
                    // 1、类名首字母小写 （这个先不实现,把这个细节先忽略）
                    // 2、如果是接口，就根据类型来匹配
                    
                    
                    Class<?>[] interfaces = clazz.getInterfaces();
                    //如果这个类实现了接口，我们就用接口的类型作为id
                    for (Class<?> i : interfaces){
                        instanceMapping.put(i.getName(),clazz.newInstance());
                    }
                }else {
                    continue;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        
    }
    
    //注入
    private void populate(){
        //首先要判断IOC容器中有没有东西
        if(instanceMapping.isEmpty()){return;}
        
        //挨个找出来看看有没有Aurowired这个标签，如果有的给它注，如果没有就忽略
        for (Map.Entry<String,Object> entry: instanceMapping.entrySet()) {
            //这时候我就们就要拿到这个类的所有的属性了，即使是私有的我们也要拿出来，所以要用强类型
            Field[] fields = entry.getValue().getClass().getDeclaredFields(); //把所有的属性全部取出来，包括私有属性。

            for (Field field: fields) {
                //判断有没有加GPAutowired注解
                if(!field.isAnnotationPresent(GPAutowired.class)){continue;}
                //如果加了就取出来，取出来判断有没有设置自定义id
                 GPAutowired autowired = field.getAnnotation(GPAutowired.class);
                
                String id = autowired.value().trim();
                //如果id为空，也就是说，自己没有设置，默认根据类型来注入
                if("".equals(id)){
                    id = field.getType().getName();
                }
                field.setAccessible(true); //把私有变量设置开放访问权限

                try {
                    field.set(entry.getValue(),instanceMapping.get(id)); //给对象设值
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    /**
     * 将首字符小写
     * @param str
     * @return
     */
    private String lowerFirstChar(String str){
        //通过ascii码来实现
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
    
    
    
//    public Object getBean(String name){
//        return null;
//    }
    
    //把这个容器所有的bean拿到
    public Map<String,Object> getAll(){
        
        return instanceMapping;
    }

    public Properties getConfig() {
        return config;
    }
}
