package przyklady06;

import java.util.LinkedList;
import java.util.Queue;

public class BlockingQueue<T> {

  final Queue<T> queue;
  final int capacity;

  public BlockingQueue(int capacity) {
    this.queue = new LinkedList<>();
    this.capacity = capacity;
  }

  public synchronized T take() throws InterruptedException {
    while(queue.isEmpty()) wait();
    var x = queue.poll();
//    System.out.println(this + " taking " + x);
    notifyAll();
    return x;
  }

  public synchronized void put(T item) throws InterruptedException {
//    System.out.println(this + " putting " + item);
    while (queue.size() + 1 > capacity) {
//      System.out.println(this + " waiting to put...");
      wait();
    }
//    System.out.println(this + " put");
    queue.add(item);
    notifyAll();
  }

  public synchronized int getSize() {
    return queue.size();
  }

  public int getCapacity() {
    return capacity;
  }
}