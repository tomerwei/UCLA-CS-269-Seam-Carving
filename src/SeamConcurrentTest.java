import java.util.ArrayList;
import java.util.List;

public class SeamConcurrentTest implements Runnable 
{
  public boolean running = false;
  SeamCarving SeamThread;
	int new_width;
	String testFileName;
  public SeamConcurrentTest (int width)
  {
	new_width = width;
	testFileName = "C:\\Users\\Brian\\workspace\\Seamcarving\\cloud_city.jpg"; //hard coded for proof of concept change to whatever file you want to resize
    Thread thread = new Thread(this);
    thread.start();
  }
  
  public static void main (String[] args) throws InterruptedException 
  {
    List<SeamConcurrentTest> workers = new ArrayList<SeamConcurrentTest>(); // threads
    
    System.out.println("This is currently running on the main thread, " +
        "the id is: " + Thread.currentThread().getId());
    // start 4 workers
    int [] defaultWidths = {111,222,333,444};  //test widths (random)
    for (int i=0; i<4; i++)
    {
      workers.add(new SeamConcurrentTest(defaultWidths[i]));
    }
    for (SeamConcurrentTest worker : workers)
    {
      while (worker.running)
      {
        Thread.sleep(100);
      }
    }
  }
  
  @Override
  public void run() 
  {
	  SeamThread = new SeamCarving(testFileName,new_width); //creates a new seamcarving class, replace this later with segmented images
    this.running = true;
    try 
    {
      Thread.sleep(5000); //sleep for 5 secs
    } 
    catch (InterruptedException e) 
    {
      Thread.currentThread().interrupt(); // if we get an interrupt stop running
    }
    this.running = false;
  }
}