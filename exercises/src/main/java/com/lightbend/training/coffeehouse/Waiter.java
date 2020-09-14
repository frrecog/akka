package com.lightbend.training.coffeehouse;

import com.lightbend.training.coffeehouse.Barista.CoffeePrepared;

import akka.actor.AbstractLoggingActor;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.Objects;

public class Waiter extends AbstractLoggingActor{

	
	private final ActorRef barista;
	private final int maxComplaintCount;
	private  int complaintCount;

	
	private final ActorRef coffeeHouse;
	
	public Waiter(ActorRef barista, int maxComplaintCount, ActorRef coffeeHouse) {
		super();
		this.barista = barista;
		this.maxComplaintCount = maxComplaintCount;
		this.coffeeHouse = coffeeHouse;
	}


	@Override
	public Receive createReceive() {
		return receiveBuilder().match(ServeCoffee.class, serveCoffee->{
			/*this.barista.tell(new Barista.PrepareCoffee(serveCoffee.coffee, sender()), self());*/
			this.coffeeHouse.tell(new CoffeeHouse.ApproveCoffee(serveCoffee.coffee, sender()), self());
			
		}).match(CoffeePrepared.class, coffeePrepared->{
			coffeePrepared.guest.tell(new CoffeeServed(coffeePrepared.coffee),self());
		}).match(Complaint.class,complaint->complaintCount==maxComplaintCount,complaint->{
			throw new FrustratedException(complaint.coffee, sender());
		}).match(Complaint.class,complaint -> {
			complaintCount++;
			this.barista.tell(new Barista.PrepareCoffee(complaint.coffee,sender()),self());

		})

				.build();
	}

	
	public static final Props props(ActorRef coffeeHouse,ActorRef barista,int maxComplaintCount) {
		return Props.create(Waiter.class, ()->new Waiter(barista, maxComplaintCount, coffeeHouse));
				}
	public static final class ServeCoffee{
		public final Coffee coffee;

		public ServeCoffee(Coffee coffee) {
			this.coffee = coffee;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((coffee == null) ? 0 : coffee.hashCode());
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
			ServeCoffee other = (ServeCoffee) obj;
			if (coffee == null) {
				if (other.coffee != null)
					return false;
			} else if (!coffee.equals(other.coffee))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "ServeCoffee [coffee=" + coffee + "]";
		}

		
		
		
		
	}
	

	public static final class CoffeeServed{
		public final Coffee coffee;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((coffee == null) ? 0 : coffee.hashCode());
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
			CoffeeServed other = (CoffeeServed) obj;
			if (coffee == null) {
				if (other.coffee != null)
					return false;
			} else if (!coffee.equals(other.coffee))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "CoffeeServed [coffee=" + coffee + "]";
		}

		public CoffeeServed(Coffee coffee) {
			super();
			this.coffee = coffee;
		}
		
		
	}
	public static final class  Complaint{
		private final Coffee coffee;

		public Complaint(Coffee coffee) {
			this.coffee = coffee;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Complaint complaint = (Complaint) o;
			return Objects.equals(coffee, complaint.coffee);
		}

		@Override
		public int hashCode() {
			return Objects.hash(coffee);
		}

		@Override
		public String toString() {
			return "Complaint{" +
					"coffee=" + coffee +
					'}';
		}
	}


	public static final class FrustratedException extends IllegalStateException{
		public final long serialVersionUID=1l;

		public final Coffee coffee;
		public final ActorRef guest;

		public FrustratedException(Coffee coffee, ActorRef guest) {
			super("Too Many complaints");
			this.coffee = coffee;
			this.guest = guest;
		}
	}
		
}
