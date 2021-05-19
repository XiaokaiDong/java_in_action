# 小马哥JAVA实战营第11周作业


## 作业内容

> 通过 Java 实现两种（以及）更多的一致性 Hash 算法
> （可选）实现服务节点动态更新


## 解答

实现如下三种一致性哈希算法，并支持节点动态更新

- 最普通的一致性哈希算法
- 带有虚拟节点的一致性哈希算法
- 带有限负载的一致性哈希算法

### 几个基础的接口

- 可哈希的对象——Hashable

```java
/**
 * 可哈希的对象
 * 可以用来表示数据，也可以用来表示节点
 */
public interface Hashable {
    /**
     * 可哈希的对象都可以返回一个整型的键值
     * @return
     */
    int getKey();
}
```

- 哈希环上的节点——HashableNode

因为节点也需要做哈希运算以确定在哈希环上的位置，所以继承自ashable

```java
/**
 * 可以放在哈希环上的数据存储节点
 * 可是是物理的，也可能是虚拟的节点
 */
public interface HashableNode extends Hashable{

    /**
     * 存储数据
     * @param data 被存储的数据
     * @throws TooBusyException 当节点压力太大时（比如达到了数据存储的上限），抛出
     *         此异常，可以用于带负载的一致性哈希
     */
    void put(Hashable data);

    /**
     * 读取数据
     * @param key 数据的键值
     * @return 目标数据
     * @throws NoSuchElementException 当节点上不存在目标数据时，抛出此异常
     */
    Hashable get(int key);

    /**
     * 移除一条数据
     * @param key 待移除数据的键值
     */
    void remove(int key);

    /**
     * 将这个数据节点与另外一个节点otherNode合并，otherNode上的数据会被迁移到本节点
     * @param otherNode 被合并的节点
     */
    void merge(HashableNode otherNode);

    /**
     * 根据一致性哈希算法，将自己保存的数据移动到目标节点上。
     * 可能没有数据需要移动。数据移动后并不是立即删除，而是采取一定的策略进行删除。
     * @param otherNode 目标数据节点
     */
    void exportDataTo(HashableNode otherNode);

}
```

- 数据路由器——Router

用于找到被存储数据所在的节点

```java
/**
 * 数据路由器，给出一个{@link Hashable}时，得到该对象应该被存储的节点
 */
public interface Router {

    /**
     * 根据数据找到对应的存储节点
     * @param data 需要被存储的数据
     * @return 该数据应该被存储的节点
     */
    HashableNode getTargetNode(Hashable data);
}
```

### 一致性哈希存储服务——ConsistentHashingDataService

其中的`void register(HashableNode dataNode)`和`void deRegister(HashableNode dataNode)`配合`HashableNode#exportDataTo`用于支持节点的动态更新。

```java
/**
 * 存储节点以及数据的管理器
 */
public interface ConsistentHashingDataService extends Router{

    /**
     * 注册一个节点。
     * 新的节点注册进来后，会引发数据的重新分布
     * @param dataNode 被注册的数据节点
     * @throws IncompatibleTypeException 当注册带有限负载的节点时，需要此Service的阶数大于0
     */
    void register(HashableNode dataNode);

    /**
     * 注销一个数据节点
     * 节点注销后，其上的数据需要分布到其它几点
     * @param dataNode 被注销的数据节点
     */
    void deRegister(HashableNode dataNode);

    /**
     * 存储数据
     * @param data 数据
     * @throws TooBusyException 无法找到空间时抛出此异常
     */
    void put(Hashable data);

    /**
     * 获取数据
     * @param key 目标数据的键值
     * @return 数据
     */
    Hashable get(int key);

    /**
     * 得到哈希环上指定节点的下一个节点
     * @param dataNode 当前节点
     * @return 下一个节点
     */
    HashableNode getNextNodeGreaterThan(HashableNode dataNode);

    /**
     * 得到哈希环上大于等于指定键的下一个节点
     * @param startKey 当前节点的键值
     * @return 下一个节点
     */
    HashableNode getNextNodeEqualOrGreaterThan(Integer startKey);

    /**
     * 得到阶数。
     * <li>如果为0，则表示不支持带有限负载的一致性哈希。</li>
     * <li>如果大于0，则表示支持带有限负载的一致性哈希。</li>
     * @return 阶数
     */
    int getOrder();
}
```

> getOrder表示带有限负载的一致性哈希存储服务。表示当前节点无法存储时应该在哈希环顺时针方向上寻找几次下一个节点。

#### ConsistentHashingDataService的默认实现——DefaultConsistentHashingDataService

这个节点可以实现上述的三种算法，算法的不同，主要体现在

- order是否大于0
  - 大于0的是带有限负载的一致性哈希，此时节点的类型为LoadLimitedHashableNode
  - 当order为0的时候
    - 如果节点为普通HashableNode，则为一般的一致性哈希算法
    - 如果节点为普通VirtualHashableNode，则为带虚拟节点的一致性哈希算法

```java
/**
 * ConsistentHashingDataManager的缺省实现类.
 * 支持带负载均衡的一致性哈希。此时order大于0，节点类型为LoadLimitedHashableNode
 * 当order == 0时：
 * <li>如果节点为普通HashableNode，则为一般的一致性哈希算法</li>
 * <li>如果节点为普通VirtualHashableNode，则为带虚拟节点的一致性哈希算法</li>
 */
public class DefaultConsistentHashingDataService implements ConsistentHashingDataService {

    private final int order;

    /**
     * 哈希环，使用Collections.synchronizedSortedMap保证线程安全
     */
    private final SortedMap<Integer, HashableNode> registry =
            Collections.synchronizedSortedMap(new TreeMap<>());

    public DefaultConsistentHashingDataService(int order) {
        this.order = order;
    }

    @Override
    public void register(HashableNode dataNode) {
        if(order > 0 && !(dataNode instanceof LoadLimitedHashableNode))
            throw new IncompatibleClassChangeError("阶数大于0时，只能注册带有限负载节点!");

        registry.putIfAbsent(dataNode.getKey(), dataNode);

        //找到哈希环上位于自己逆时针方向之后的第一个节点
        HashableNode nextNode = getNextNodeGreaterThan(dataNode);

        //迁移数据
        nextNode.exportDataTo(dataNode);
    }

    @Override
    public HashableNode getNextNodeGreaterThan(HashableNode dataNode) {
        HashableNode nextNode = null;
        SortedMap<Integer, HashableNode> tailMap = registry.tailMap(dataNode.getKey());
        for (Integer key : tailMap.keySet()) {
            if (key > dataNode.getKey()){
                nextNode = tailMap.get(key);
                break;
            }
        }
        return nextNode;
    }

    @Override
    public HashableNode getNextNodeEqualOrGreaterThan(Integer startKey) {
        HashableNode nextNode = null;
        SortedMap<Integer, HashableNode> tailMap = registry.tailMap(startKey);
        for (Integer key : tailMap.keySet()) {
            nextNode = tailMap.get(key);
            break;
        }
        return nextNode;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public void deRegister(HashableNode dataNode) {
        //注销之前先找到后继（顺时针方向）节点
        HashableNode nextNode = getNextNodeGreaterThan(dataNode);

        //将被注销的节点dataNode的数据导给下一个节点
        dataNode.exportDataTo(nextNode);

        //将自己从注册表中删除
        registry.remove(dataNode.getKey());
    }

    @Override
    public void put(Hashable data) {
        HashableNode targetNode = getTargetNode(data);
        if(order == 0) {
            targetNode.put(data);
        } else {
            //此时为带有限负载的一致性哈希
            LoadLimitedHashableNode loadLimitedNode = (LoadLimitedHashableNode)targetNode;
            if (loadLimitedNode.canTakeOne()) {
                loadLimitedNode.put(data);
            } else {
                LoadLimitedHashableNode nextNode =
                        (LoadLimitedHashableNode)getNextNodeGreaterThan(loadLimitedNode);
                int i = 0;
                while(!nextNode.canTakeOne() && i < order) {
                    nextNode = (LoadLimitedHashableNode)getNextNodeGreaterThan(nextNode);
                    i++;
                }
                if(i < order)
                    nextNode.put(data);
                else
                    throw new TooBusyException("经过" + i + "次寻找后，无法找到更多的空间！");
            }
        }
    }

    @Override
    public Hashable get(int key) {
        Hashable result = null;

        //无论是否带有限负载，直接在最近的节点上寻找数据
        HashableNode currNode = getNextNodeEqualOrGreaterThan(key);
        result = currNode.get(key);

        if (result == null && order > 0){
            //否则最多在哈希环顺时针方向上order个节点上寻找数据
            int i = 0;
            while (i < order) {
                currNode = getNextNodeGreaterThan(currNode);
                result = currNode.get(key);
                if (result != null) {
                    break;
                }
                i ++;
            }
        }

        return result;
    }

    @Override
    public HashableNode getTargetNode(Hashable data) {
        Integer key = data.getKey();

        return getNextNodeEqualOrGreaterThan(key);
    }
}
```

### 最普通的一致性哈希算法

实现类是DefaultConsistentHashingDataService，此时order为0，节点是普通的HashableNode

### 带有限负载的一致性哈希算法

实现类是DefaultConsistentHashingDataService，此时order大于0，管理的节点是LoadLimitedHashableNode。


```java
/**
 * 有存储上限的存储节点，用于实现“带有限负载的一致性哈希”。
 * 当数据按照一致性哈希算法找到相应的存储节点时，要先判断该存储节点是否达到了存储上限；
 * 如果已经达到了存储上限，则需要继续寻找该节点顺时针方向之后的节点进行存储.
 * 所谓的Ordered是指寻找下一节点的次数，比如寻找一次就是一阶，等等。
 *
 * 寻找的动作由上层的Service来进行。寻找的次数由"阶数Ordered"指定。
 * 所谓的Ordered是指寻找下一节点的次数，比如寻找一次就是一阶，等等。
 */
public class LoadLimitedHashableNode implements HashableNode, HashingDataServiceAware {

    /**
     * 预测是否达到上限
     */
    private final Predicate<LoadLimitedHashableNode> canTakeMoreData;

    private ConsistentHashingDataService dataService;

    /**
     * 对应的物理节点，用于真正保存数据
     */
    final private HashableNode originalNode;

    //本节点的编号
    private int nodeNo;

    public LoadLimitedHashableNode(Predicate<LoadLimitedHashableNode> canTakeMoreData,
                                   HashableNode originalNode) {
        this.canTakeMoreData = canTakeMoreData;
        this.originalNode = originalNode;
    }

    /**
     *
     * @param data 被存储的数据
     * @throws TooBusyException, 当无法找到更多空间时抛出这种异常。上层Service捕获后，
     * 寻找哈希环上下一个节点。
     */
    @Override
    public void put(Hashable data) {
        if (canTakeMoreData.test(this)) {
            originalNode.put(data);
        } else {
            throw new TooBusyException("空间不足");

        }
    }

    public boolean canTakeOne() {
        return canTakeMoreData.test(this);
    }

    @Override
    public Hashable get(int key) {
        return originalNode.get(key);
    }

    @Override
    public void remove(int key) {
        originalNode.remove(key);
    }

    @Override
    public void merge(HashableNode otherNode) {
        originalNode.merge(otherNode);
    }

    @Override
    public void exportDataTo(HashableNode otherNode) {
        originalNode.exportDataTo(otherNode);
    }

    @Override
    public int getKey() {
        return originalNode.getKey();
    }

    @Override
    public void setDataServiceAware(ConsistentHashingDataService dataService) {
        this.dataService = dataService;
    }
}
```

- 存储数据

使用Predicate<LoadLimitedHashableNode> canTakeMoreData来判断是否还可以保存数据。如果不可以保存数据，则DefaultConsistentHashingDataService会在哈希环上寻找顺时针方向的下一个节点，最多寻找order次。

```java
    @Override
    public void put(Hashable data) {
        HashableNode targetNode = getTargetNode(data);
        if(order == 0) {
            targetNode.put(data);
        } else {
            //此时为带有限负载的一致性哈希
            LoadLimitedHashableNode loadLimitedNode = (LoadLimitedHashableNode)targetNode;
            if (loadLimitedNode.canTakeOne()) {
                loadLimitedNode.put(data);
            } else {
                LoadLimitedHashableNode nextNode =
                        (LoadLimitedHashableNode)getNextNodeGreaterThan(loadLimitedNode);
                int i = 0;
                while(!nextNode.canTakeOne() && i < order) {
                    nextNode = (LoadLimitedHashableNode)getNextNodeGreaterThan(nextNode);
                    i++;
                }
                if(i < order)
                    nextNode.put(data);
                else
                    throw new TooBusyException("经过" + i + "次寻找后，无法找到更多的空间！");
            }
        }
```

- 读取数据

如果当前节点没有相应的数据，会在哈希环上顺时针方向最多寻找order次

```java
    @Override
    public Hashable get(int key) {
        Hashable result = null;

        //无论是否带有限负载，直接在最近的节点上寻找数据
        HashableNode currNode = getNextNodeEqualOrGreaterThan(key);
        result = currNode.get(key);

        if (result == null && order > 0){
            //否则最多在哈希环顺时针方向上order个节点上寻找数据
            int i = 0;
            while (i < order) {
                currNode = getNextNodeGreaterThan(currNode);
                result = currNode.get(key);
                if (result != null) {
                    break;
                }
                i ++;
            }
        }

        return result;
    }
```

### 带虚拟节点的一致性哈希

实现类是DefaultConsistentHashingDataService，此时order等于0，管理的节点是VirtualHashableNode。

VirtualHashableNode其实是一个普通HashableNode的装饰器，会逻辑保存一份数据，真实的数据存储委托给真正的底层HashableNode。

```java
public class VirtualHashableNode implements HashableNode{
    /**
     * 对应的物理节点，用于真正保存数据
     */
    final private HashableNode originalNode;
    //...

    /**
     * 用来保存数据的键值，完整的数据存储在物理节点上
     * 使用了CopyOnWriteArraySet，表示默认情况下读多写少。此处可以更加的抽象
     */
    Set<Integer> dataSlot = new CopyOnWriteArraySet<>();

    @Override
    public void put(Hashable data) {
        //保存数据的键值
        dataSlot.add(data.getKey());
        //真正存储数据
        originalNode.put(data);
    }

    @Override
    public Hashable get(int key) {
        Hashable result = null;
        if(dataSlot.contains(key))
            result = originalNode.get(key);

        return result;
    }
}
```

在迁移数据时，实际是往底层存储节点上迁移。

```java
   @Override
    public void exportDataTo(HashableNode otherNode) {
        for (int key : dataSlot) {
            //先将数据保存到目标节点
            otherNode.put(get(key));
            //然后从当前节点上删除此条数据
            //  先逻辑删除
            dataSlot.remove(key);
            //  再物理删除
            originalNode.remove(key);
        }
    }
```

不支持节点的合并

```java
    @Override
    public void merge(HashableNode otherNode) {
        //虚拟节点不支持合并
        return;
    }
```