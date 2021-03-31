# 小马哥JAVA实战营第五周作业


## 作业内容1


> 修复本程序 org.geektimes.reactive.streams.DefaultSubscriber#onNext


- 原始代码如下

  ```java
    @Override
    public void onNext(Object o) {
        if (++count > 2) { // 当到达数据阈值时，取消 Publisher 给当前 Subscriber 发送数据
            subscription.cancel();
            return;
        }
        System.out.println("收到数据：" + o);
    }
  ```

  这里onNext的目的是处理下一个数据元素，即只要进到这个方法的数据，就是要处理的，所以在if语句中不应该直接返回，而是仍应该打印。此时虽然打印了数据，但是标记了状态，下次就会停止处理。

  修复方法就是将return语句注释掉

  ```java
    @Override
    public void onNext(Object o) {
        if (++count > 2) { // 当到达数据阈值时，取消 Publisher 给当前 Subscriber 发送数据
            subscription.cancel();
            //return;
        }
        System.out.println("收到数据：" + o);
    }
  ```

## 作业内容2

> 继续完善 my-rest-client POST 方法

- “优化”原始的DefaultInvocationBuilder为MyInvocationBuilder，统一处理有报文体和没有报文体的情况。

  - DefaultInvocationBuilder的关键代码如下

    ```java
    @Override
    public Invocation buildGet() {
        return new HttpGetInvocation(uriBuilder.build(), headers);
    }

    @Override
    public Invocation buildDelete() {
        return null;
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return null;
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return null;
    }
    ```

  - MyInvocationBuilder变为

    ```java
    @Override
    public Invocation buildGet() {
        return buildRequestHelper(null, HttpMethod.GET);
    }

    @Override
    public Invocation buildDelete() {
        return buildRequestHelper(null, HttpMethod.GET);
    }

    @Override
    public Invocation buildPost(Entity<?> entity) {
        return buildRequestHelper(entity, HttpMethod.POST);
    }

    @Override
    public Invocation buildPut(Entity<?> entity) {
        return buildRequestHelper(entity, HttpMethod.PUT);
    }
    ```

    即统一调用helper方法buildRequestHelper

    ```java
    private Invocation buildRequestHelper(Entity<?> entity, String methodName){
        MyHttpInvocation myHttpInvocation = new MyHttpInvocation(uriBuilder.build(), headers);
        if (entity != null)
            myHttpInvocation.setEntity(entity);
        if (methodName == null || methodName.isEmpty())
            //默认为GET请求
            myHttpInvocation.setHttpMethod(HttpMethod.GET);
        else
            myHttpInvocation.setHttpMethod(methodName);
        return myHttpInvocation;
    }
    ```
    
    可以看到用MyHttpInvocation代替了原来的HttpGetInvocation

- MyHttpInvocation

  - 增加httpMethod来保存本次HTTP请求使用的方法

  ```java
  public void setHttpMethod(String httpMethod) {
      this.httpMethod = httpMethod;
  }

  private String httpMethod;
  ```

  - 增加报文体相关属性

    ```java
    public void setEntity(Entity<?> entity) {
        this.entity = entity;
    }

    /**
     * For HTTP body
     */
    Entity<?> entity;
    ```

  - 修改invoke方法以处理报文体即报文体的类型

    ```java
    @Override
    public Response invoke() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(httpMethod);
            setRequestHeaders(connection);
            connection.setDoOutput(true);

            //写入报文体
            if(entity != null) {
                connection.setRequestProperty("Accept", entity.getMediaType().getType() + "/" + entity.getMediaType().getSubtype());
                connection.setRequestProperty("Content-type", entity.getMediaType().getType() + "/" + entity.getMediaType().getSubtype());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), "UTF-8"));
                writer.write((String)entity.getEntity());
                writer.close();
            }
            // TODO Set the cookies
            int statusCode = connection.getResponseCode();
            DefaultResponse response = new DefaultResponse();
            response.setConnection(connection);
            response.setStatus(statusCode);
            return response;

          } catch (IOException e) {
              // TODO Error handler
          }
          return null;
    }
    ```
  
  - 然后就可以测试了

    ```java
    Client client = ClientBuilder.newClient();

    Response res = client
            .target("http://127.0.0.1:8080/hello/world")
            .request()
            .post(Entity.text("123"));
    ```

    通过wireshark抓包，可以看到POST报文发送成功，报文头中报文类型符合预期

    ```
    Hypertext Transfer Protocol
      POST /hello/world HTTP/1.1\r\n
      Accept: text/plain\r\n
      Content-type: text/plain\r\n
      User-Agent: Java/1.8.0_161\r\n
      Host: 127.0.0.1:8080\r\n
      Connection: keep-alive\r\n
      Content-Length: 3\r\n
      \r\n
      [Full request URI: http://127.0.0.1:8080/hello/world]
      [HTTP request 1/1]
      [Response in frame: 23]
      File Data: 3 bytes
    Line-based text data: text/plain (1 lines)
      123
    ```
