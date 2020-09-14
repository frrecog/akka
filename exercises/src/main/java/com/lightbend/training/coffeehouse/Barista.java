package com.lightbend.training.coffeehouse;

import akka.actor.AbstractActorWithStash;
import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import scala.concurrent.duration.FiniteDuration;

import java.util.Random;

public class Barista extends AbstractActorWithStash {

	private final FiniteDuration prepareCoffeeDuration;

	private final int accuracy;
	
	public Barista(FiniteDuration prepareCoffeeDuration, int accuracy) {
		super();
		this.prepareCoffeeDuration = prepareCoffeeDuration;
		this.accuracy = accuracy;
	}
	
	public static final Props props(FiniteDuration prepareCoffeeDuration,final int accuracy) {
	 return Props.create(Barista.class, ()->new Barista(prepareCoffeeDuration, accuracy));
	}

	private Receive ready() {
		return receiveBuilder().match(PrepareCoffee.class, prepareCoffee->{
			/*Busy.busy(prepareCoffeeDuration);
			sender().tell(new Barista.CoffeePrepared(pickCoffee(prepareCoffee.coffee), prepareCoffee.guest), self());*/
			scheduleCoffeePrepared(prepareCoffee.coffee, prepareCoffee.guest);
			getContext().become(busy(sender()));

		}).matchAny(this::unhandled)
				.build();
	}

	private Receive busy(ActorRef waiter) {
		return receiveBuilder().match(CoffeePrepared.class, coffeePrepared -> {
					waiter.tell(coffeePrepared, self());
					unstashAll();
					getContext().become(ready());
				}
		).matchAny(msg -> stash())
				.build();
	}

	private void scheduleCoffeePrepared(Coffee coffee, ActorRef guest) {
		context().system().scheduler().scheduleOnce(prepareCoffeeDuration, self(),
				new CoffeePrepared(pickCoffee(coffee), guest), context().dispatcher(), self());
	}
	
	@Override
	public Receive createReceive() {
		return ready();
	}

	private Coffee pickCoffee(Coffee coffee)
	{
		return new Random().nextInt(100)<accuracy?coffee:Coffee.orderOther(coffee);
	}

	public static final class PrepareCoffee
	{
		public final Coffee  coffee;
		public final ActorRef guest;
		public PrepareCoffee(Coffee coffee, ActorRef guest) {
			super();
			this.coffee = coffee;
			this.guest = guest;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((coffee == null) ? 0 : coffee.hashCode());
			result = prime * result + ((guest == null) ? 0 : guest.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			PrepareCoffee other = (PrepareCoffee) obj;
			if (coffee == null) {
				if (other.coffee != null)
					return false;
			} else if (!coffee.equals(other.coffee))
				return false;
			if (guest == null) {
				if (other.guest != null)
					return false;
			} else if (!guest.equals(other.guest))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return "PrepareCoffee [coffee=" + coffee + ", guest=" + guest + "]";
		}
		
		
	}
	
	public static final class CoffeePrepared
	{
		public final Coffee  coffee;
		public final ActorRef guest;
		public CoffeePrepared(Coffee coffee, ActorRef guest) {
			super();
			this.coffee = coffee;
			this.guest = guest;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((coffee == null) ? 0 : coffee.hashCode());
			result = prime * result + ((guest == null) ? 0 : guest.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CoffeePrepared other = (CoffeePrepared) obj;
			if (coffee == null) {
				if (other.coffee != null)
					return false;
			} else if (!coffee.equals(other.coffee))
				return false;
			if (guest == null) {
				if (other.guest != null)
					return false;
			} else if (!guest.equals(other.guest))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return "PrepareCoffee [coffee=" + coffee + ", guest=" + guest + "]";
		}
		
		
	}
}
