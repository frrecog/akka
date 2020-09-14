package com.lightbend.training.coffeehouse;

import com.lightbend.training.coffeehouse.Waiter.ServeCoffee;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.FiniteDuration;

public class Guest extends AbstractLoggingActorWithTimer {

	
	private final ActorRef waiter;
	private final Coffee favoriteCoffee;
	private int coffeeCount=0;
	private final FiniteDuration coffeeFinishedDuration;
	private final int caffineLimit;
	
	
	public Guest(ActorRef waiter, Coffee favoriteCoffee, FiniteDuration coffeeFinishedDuration, int caffineLimit) {
		super();
		this.waiter = waiter;
		this.favoriteCoffee = favoriteCoffee;
		this.coffeeFinishedDuration = coffeeFinishedDuration;
		this.caffineLimit= caffineLimit;
		orderFavoriteCoffee();
		
	}


	@Override
	public Receive createReceive() {
		
		return receiveBuilder()
				.match(Waiter.CoffeeServed.class,coffeeServed -> coffeeServed.coffee.equals(favoriteCoffee), coffeeServed -> {
					coffeeCount++;
					log().info("Enjoying my {} yummy this {}!", coffeeCount, coffeeServed.coffee);
					scheduleCoffeeFinished();
				}).match(Waiter.CoffeeServed.class,coffeeServed -> {
					log().info("Expected {} but got {}",favoriteCoffee,coffeeServed.coffee);
					this.waiter.tell(new Waiter.Complaint(favoriteCoffee),self());
				})
				.match(CoffeeFinished.class,coffeeFinished -> coffeeCount>caffineLimit,coffeeFinished -> {
					throw new CaffeineException();
				})
				.match(CoffeeFinished.class, coffeeFinished-> orderFavoriteCoffee()).build();
	}
	
	private void orderFavoriteCoffee()
	{
		this.waiter.tell(new Waiter.ServeCoffee(this.favoriteCoffee), self());
		
	}
	private void scheduleCoffeeFinished() {
		getTimers().startSingleTimer("coffee-finished", CoffeeFinished.Instance, coffeeFinishedDuration);
	}
	
	public static Props props(final ActorRef waiter,final Coffee favoriteCoffee, final FiniteDuration coffeeFinishedDuration,final  int caffineLimit) {
		return Props.create(Guest.class,()->new Guest(waiter, favoriteCoffee, coffeeFinishedDuration,caffineLimit));
	}

	
	public static final class CoffeeFinished{
		public static final CoffeeFinished Instance=new CoffeeFinished();
		private CoffeeFinished() {
			
		}
	}
	public static final class CaffeineException  extends IllegalStateException{
		static final long serialVersionUID=1l;
		public CaffeineException( ) {
			super("Too much caffine");
		}


	}
	@Override
	public void postStop() {
		log().info("Good bye");
	}
}
