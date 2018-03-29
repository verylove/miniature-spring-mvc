package com.gupaoedu.vip.mvc.framework.servlet;

import com.gupaoedu.vip.mvc.framework.annotation.GPController;
import com.gupaoedu.vip.mvc.framework.annotation.GPRequestMapping;
import com.gupaoedu.vip.mvc.framework.annotation.GPRequestParam;
import com.gupaoedu.vip.mvc.framework.context.GPAppliactionContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author lrj
 * @see
 * @since 2018.01.16
 */
public class GPDdispathcerServlet extends HttpServlet{

    //web.xml配置的常量
    private static final String LOCATION = "contextConfigLocation";
    
    //原来的：private Map<String,Handler> handlerMapping = new HashMap<>();
    //改为正则的：
    //private Map<Pattern,Handler> handlerMapping = new HashMap<>();
    //handlerMapping改成list
    private List<Handler> handlerMapping = new ArrayList<>();
    
    //初始化适配器之后，就保存下来
    private Map<Handler,HandlerAdapter> adapterMapping = new HashMap<>();


    private List<ViewResolver> viewResolvers = new ArrayList<>();


    //初始化我们的IOC容器
    @Override
    public void init(ServletConfig config) throws ServletException {
        
        //IOC容器必须要先初始化
        //假装容器已启动
        GPAppliactionContext context = new GPAppliactionContext(config.getInitParameter(LOCATION));

        Map<String,Object> ioc = context.getAll();
        
        System.out.println(ioc);

        System.out.println(ioc.get("firstAction"));

        //请求解析
        initMultipartResolver(context);
        //多语言、国际化
        initLocaleResolver(context);
        //主题View层的
        initThemeResolver(context);
       
        //=============================重要=========================
        //解析url和Method的关联关系，U（Url）R（Relation）M（Mapping），数据库的就叫ORM(ObjectRelationMapping)----主要
        initHandlerMappings(context);
        //适配器（匹配的过程）----主要
        initHandlerAdapters(context);
        //=============================重要=========================
        
        //异常解析
        initHandlerExceptionResolvers(context);
        //视图转发（根据视图名字匹配到一个具体模板）
        initRequestToViewNameTranslator(context);

        //=============================重要=========================
        //解析模板中的内容（拿到服务器传过来的数据，生产HTML代码）
        initViewResolvers(context);
        //=============================重要=========================

        initFlashMapManager(context);

        System.out.println("Hello World ----- Spring MVC is init.");
        
    }
    //请求解析
    private void initMultipartResolver(GPAppliactionContext context){}
    
    //多语言、国际化
    private void initLocaleResolver(GPAppliactionContext context){}
    
    //主题View层的
    private void initThemeResolver(GPAppliactionContext context){}
    
    //解析url和Method的关联关系，U（Url）R（Relation）M（Mapping），数据库的就叫ORM(ObjectRelationMapping)----主要
    private void initHandlerMappings(GPAppliactionContext context){
        Map<String,Object> ioc = context.getAll();
        if(ioc.isEmpty()){
            return;
        }
        //只要是由Controller修饰的类，里面的方法全部找出来，而且这个方法上应该要加上了ReuqestMapping注解，如果没加这个注解，这个方法是不能被外界来访问的
        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(GPController.class)){
                continue;
            }
            String url = "";
            if(clazz.isAnnotationPresent(GPController.class)){
                GPRequestMapping requestMapping = clazz.getAnnotation(GPRequestMapping.class);
                url = requestMapping.value();
            }
            //扫描Controller下面的所有方法
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if(!method.isAnnotationPresent(GPRequestMapping.class)){
                    continue;
                }
                GPRequestMapping requestMapping = method.getAnnotation(GPRequestMapping.class);
                //原来的 : String mapingUrl = (url + requestMapping.value());
                //改造成正则的
                String regex = (url + requestMapping.value()).replaceAll("/+","/");
                Pattern pattern = Pattern.compile(regex);
                
                //原来的：handlerMapping.put(mapingUrl,new Handler(entry.getValue(),method));
                //改为正则的
                //handlerMapping.put(pattern,new Handler(entry.getValue(),method));
                //当handlerMapping改成list后
                handlerMapping.add(new Handler(pattern,entry.getValue(),method));

                //原来的：System.out.println("Mapping: " + mapingUrl +  " " + method.toString());
                //改成正则的：System.out.println("Mapping: " + mapingUrl +  " " + method.toString());
                System.out.println("Mapping: " + regex +  " " + method.toString());
            }
            
            
        }
        
        //RequestMapping会配置一个url，那么一个url就对应一个方法，并将这个关系保存到map中
        
        
    }
    //适配器（匹配的过程）----主要
    //这个Adapters主要是用来动态匹配我们的参数的，还有动态赋值（这里是初始化，不赋值）
    private void initHandlerAdapters(GPAppliactionContext context){
        if(handlerMapping.isEmpty()){return;}
        //参数类型作为key，参数的索引号作为值
        Map<String,Integer> paramMapping = new HashMap<>();
        
        //只需要取出来具体的某个方法
        //原来的：for (Map.Entry<String,Handler> entry :handlerMapping.entrySet()) {
        //改成正则的
//        for (Map.Entry<Pattern,Handler> entry :handlerMapping.entrySet()) {
        //当handlerMapping改成list后
        for (Handler handler :handlerMapping) {
            //把这个方法上面所有的参数全部获取到
            Class<?>[] paramsTypes = handler.method.getParameterTypes();
            //参数是有顺序的，但是通过反射，没法拿到我们参数名字，要通过序号来确定，所以用int i =0 的循环,匹配自定义参数列表
            for (int i = 0 ; i < paramsTypes.length ; i++ ) {
                Class<?> type = paramsTypes[i];
                // 等于就HttpServletRequest、HttpServletResponse就将类型put进去
                if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramMapping.put(type.getName(),i);    
                }
            }
            //不等于，就要使用注解了,拿到一个注解,这里是匹配我们的Request和Response
            Annotation[][] pa = handler.method.getParameterAnnotations(); //这个注解是二维数组，因为每个参数上可以加多个注解的
            for (int i = 0 ; i < pa.length ; i++){
                for(Annotation a : pa[i]){
                    if(a instanceof GPRequestParam){
                        String paramName = ((GPRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramMapping.put(paramName,i);
                        }
                        
                    }
                }
            }
            adapterMapping.put(handler,new HandlerAdapter(paramMapping));
        }
    }
    
    //异常解析
    private void initHandlerExceptionResolvers(GPAppliactionContext context){}
    
    //视图转发（根据视图名字匹配到一个具体模板）
    private void initRequestToViewNameTranslator(GPAppliactionContext context){}
    
    //解析模板中的内容（拿到服务器传过来的数据，生产HTML代码）
    private void initViewResolvers(GPAppliactionContext context){
        
        //模板一般是不会放到WebRoot下的，二是放在WEB-INF下，或者class下，这样就避免了用户直接请求到模板
        //加载模板的个数，存储到缓存中，检查模板中的语法错误
        String templateRoot = context.getConfig().getProperty("templateRoot");
        
        //归根到底就是一个文件，普通文件
        String rootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        
        File rootDir = new File(rootPath);
        for (File template : rootDir.listFiles()) {
            viewResolvers.add(new ViewResolver(template.getName(),template));
            
        }
        
        
        
        
    }

    private void initFlashMapManager(GPAppliactionContext context){}
    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    //在这里调用自己写的Controller的方法
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req,resp);
        }catch (Exception e){
            resp.getWriter().write("500 Exception, Msg :" + Arrays.toString(e.getStackTrace()));
        }
        
    }
    
    private Handler getHandler(HttpServletRequest req){
        //循环handlerMapping
        if(handlerMapping.isEmpty()){return null;}
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        //将contextPath替换为空字符，如果用户+了多个/（斜杠）就替换成一个斜杠
        url = url.replace(contextPath,"").replaceAll("/+","/");

        //改成正则后，动态匹配
        //for (Map.Entry<Pattern,Handler> entry :handlerMapping.entrySet()) {
        //当handlerMapping改成list后
        for (Handler handler :handlerMapping) {
            Matcher matcher = handler.pattern.matcher(url);
            
            if(!matcher.matches()){continue;}
            
            return handler;
        }
        return null;
    }
    
    private HandlerAdapter getHandlerAdapter(Handler handler){
        if(adapterMapping.isEmpty()){return null;}
        
        return adapterMapping.get(handler);
    }
    
    private void doDispatch(HttpServletRequest req,HttpServletResponse resp) throws Exception{
        try{
            //先取出一个Handler从HandlerMapping里取
            Handler handler = getHandler(req);
            if(handler == null){
                resp.getWriter().write("404 Not Found");
                return;
            }
            //再取出来一个适配器
            //再又适配器去调用我们具体的方法
            HandlerAdapter ha = getHandlerAdapter(handler);
            GPModelAndView mv = ha.handle(req,resp,handler);
            
            //写一个咕泡模板框架 Velocity：# Freemarker：# jsp：${name} 咕泡模板就用 @{name}
            applyDefaultViewName(resp, mv);
            
        }catch (Exception e){
            throw e;
        }
        
    }
    
    //这个方法就是根据用户设置的一个值找到我们对应的模板 
    public void applyDefaultViewName(HttpServletResponse resp,GPModelAndView mv) throws Exception{
        if(null == mv){return;}
        if (viewResolvers.isEmpty()){return;}
        for (ViewResolver resolver : viewResolvers) {
            if(!mv.getView().equals(resolver.getViewName())){
                continue;
            }
            String r = resolver.parse(mv);
        
            if(r != null){
                resp.getWriter().write(r);
                break;
            }
        }
    }
    

    /**
     * 方法适配器
     */
    private class HandlerAdapter{
        private Map<String,Integer> paramMapping;
        
        public HandlerAdapter(Map<String,Integer> paramMapping){
            this.paramMapping = paramMapping;
        }
        
        //主要目的是用反射调用调用url对应的method
        public GPModelAndView handle(HttpServletRequest req, HttpServletResponse resp, Handler handler) throws Exception{
            
            //为什么要传req、为什么要传resp、为什么要传handler
            Class<?>[] paramTypes = handler.method.getParameterTypes();
            
            //要想给参数赋值，只能通过索引号来找到具体的某个参数
            //把参数全部拿到
            Object [] paramValues = new Object[paramTypes.length];
            //声明一个参数值的数组
            Map<String,String[]> params = req.getParameterMap();
            for (Map.Entry<String,String[]> param :params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","").replaceAll(",\\s",",");
                if(!this.paramMapping.containsKey(param.getKey())){
                    continue;
                }
                int index = this.paramMapping.get(param.getKey());
                //单个赋值是不行的
                paramValues[index] = castStringValue(value,paramTypes[index]);
            }
            
            //request 和 response 要赋值
            String reqName = HttpServletRequest.class.getName();
            if (this.paramMapping.containsKey(reqName)){
                int reqIndex = this.paramMapping.get(reqName);
                paramValues[reqIndex] = req;
            }

            String respName = HttpServletResponse.class.getName();
            if (this.paramMapping.containsKey(respName)){
                int respIndex = this.paramMapping.get(respName);
                paramValues[respIndex] = resp;
            }
            
            boolean isModelAndView = handler.method.getReturnType() == GPModelAndView.class;
            Object r = handler.method.invoke(handler.controller,paramValues);
            if(isModelAndView){
                return (GPModelAndView)r;
            }else {
                return null;
            }
        }
        
        private Object castStringValue(String value, Class<?> clazz){
            if (clazz == String.class){
                return value;
            }else if (clazz == Integer.class){
                return Integer.valueOf(value);
            }else if(clazz == int.class){
                return Integer.valueOf(value).intValue();
            }else {
                return null;
            }
        }
        
    }

    /**
     * HandlerMapping 定义
     */
    private class Handler{
        
        protected Object controller;
        protected Method method;
        protected Pattern pattern;
        
        protected Handler(Pattern pattern,Object controller,Method method){
            //使用正则后，直接把handlerMapping改成list，在Handler内部类加多一个pattern属性
            this.pattern = pattern;
            this.controller = controller;
            this.method = method;
        }
        
    }
    
    /**
     * 解析器
     */
    private class ViewResolver{
        private String viewName;
        
        private File file;
        
        protected ViewResolver(String viewName,File file){
            this.viewName = viewName;
            this.file = file;
        }

        protected String parse(GPModelAndView mv) throws Exception{

            StringBuffer sb = new StringBuffer();
            
            RandomAccessFile ra = new RandomAccessFile(this.file,"r");
            try{
                //模板框架的语法是非常复杂的，但是原理是一样的，无非都是用正则表达式来处理字符串而已，就这么简单，不要认为这个模板框架的语法是有多么的高大上
                String line = null;
                while (null != (line = ra.readLine())){
                    Matcher m = matcher(line);
                    while (m.find()){
                        //正则分组，m.groupCount()代表一组有多少个
                        for (int i = 1; i <= m.groupCount(); i++) {
                            String paramName = m.group(i); //把这个组取出来，就是得到参数名字
                            Object paramValue = mv.getModel().get(paramName);
                            if(null == paramValue){continue;}   
                            line = line.replaceAll("@\\{" + paramName + "\\}",paramValue.toString());
                        }
                    }
                    sb.append(line);
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                ra.close();
            }
            
            return sb.toString();
        }
        
        private Matcher matcher(String str){
            Pattern pattern = Pattern.compile("@\\{(.+?)\\}",Pattern.CASE_INSENSITIVE);
            Matcher m = pattern.matcher(str);
            return m;
        }

        public String getViewName() {
            return viewName;
        }
    }
    
}












