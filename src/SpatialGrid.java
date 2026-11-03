import java.util.*;

/** Transient ordered local index: query results preserve original encounter order. */
final class SpatialGrid<T> {
    private record Entry<T>(T value,double x,double y,long order){}
    private final Map<Long,ArrayList<Entry<T>>> cells=new HashMap<>();
    private final IdentityHashMap<T,Entry<T>> entries=new IdentityHashMap<>();
    private long next;
    private static long key(int x,int y){return ((long)x<<32)^(y&0xffffffffL);}
    private static int cell(double v){return (int)Math.floor(v/100);}
    void add(T value,double x,double y){Entry<T> old=entries.get(value);long order=old==null?next++:old.order;remove(value);Entry<T> e=new Entry<>(value,x,y,order);entries.put(value,e);cells.computeIfAbsent(key(cell(x),cell(y)),k->new ArrayList<>()).add(e);}
    void remove(T value){Entry<T> e=entries.remove(value);if(e!=null){var bucket=cells.get(key(cell(e.x),cell(e.y)));bucket.remove(e);if(bucket.isEmpty())cells.remove(key(cell(e.x),cell(e.y)));}}
    List<T> near(double x,double y,double radius){ArrayList<Entry<T>> result=new ArrayList<>();double r2=radius*radius;for(int cy=cell(y-radius);cy<=cell(y+radius);cy++)for(int cx=cell(x-radius);cx<=cell(x+radius);cx++){var b=cells.get(key(cx,cy));if(b!=null)for(var e:b)if((e.x-x)*(e.x-x)+(e.y-y)*(e.y-y)<=r2)result.add(e);}result.sort(Comparator.comparingLong(Entry::order));return result.stream().map(Entry::value).toList();}
}
