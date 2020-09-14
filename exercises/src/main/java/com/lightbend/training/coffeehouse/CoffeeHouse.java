package com.lightbend.training.coffeehouse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import akka.actor.*;
import akka.japi.pf.DeciderBuilder;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.FromConfig;
import scala.concurrent.duration.FiniteDuration;


public class CoffeeHouse extends AbstractLoggingActor {

	private final FiniteDuration prepareCoffeeDuration = FiniteDuration.create(
			context().system().settings().config().getDuration(
						"coffee-house.barista.prepare-coffee-duration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
			
	
	private final FiniteDuration coffeeFinishedDuration = FiniteDuration.create(
		context().system().settings().config().getDuration(
					"coffee-house.guest.finish-coffee-duration", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);

	private final int  baristaAccuracy = context().system().settings().config()
			.getInt("coffee-house.barista.accuracy");

	private final int  waiterMaxComplaintCount = context().system().settings().config()
			.getInt("coffee-house.waiter.max-complaint-count");

	
	private  final ActorRef barista=createBarista();
	private  final ActorRef waiter=createWaiter();
	
	private final Map<ActorRef, Integer> guestbook=new HashMap<>(); 
	private final int caffineLimit;
	
	
	public CoffeeHouse(int caffineLimit) {
		this.caffineLimit = caffineLimit;
		log().debug("CoffeHouse Open");
	}


	@Override
	public Receive createReceive() {
	
	//	return ReceiveBuilder.create().matchAny(msg -> log().info("Coffee brewing")).build();
	//	return ReceiveBuilder.create().matchAny(msg -> sender().tell(msg.toString(), self())).build();
	//	return ReceiveBuilder.create().match(CreateGuest.class, createGust->createGuest(createGust.favoriteCoffee)).build();
		
		return ReceiveBuilder.create().match(CreateGuest.class, createGust->{
			ActorRef guest=createGuest(createGust.favoriteCoffee,createGust.guestCaffineLimit);
			addGuestToGuestBook(guest);
			context().watch(guest);
			}).match(ApproveCoffee.class, this::approveCoffee, approveCoffee -> {
				barista.forward(new Barista.PrepareCoffee(approveCoffee.coffee, approveCoffee.guest), context());
			}).match(ApproveCoffee.class, approveCoffee -> {
				log().info("Sorry {} but you have reached your limits", approveCoffee.guest);
				context().stop(approveCoffee.guest);
			}).match(Terminated.class,terminated->{
                log().info("Thanks, {} for being our best coffee guest", terminated.getActor());
                removeGuestToGuestBook(terminated.getActor());
		    }).match(GetStatus.class,getStatus -> {
		        sender().tell(new Status(context().children().size()-2),self());
        })
				
				.build();
		
		
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {

		return new OneForOneStrategy(true, DeciderBuilder.
				match(Guest.CaffeineException.class,e->SupervisorStrategy.stop())
				.match(Waiter.FrustratedException.class, e -> {
					barista.tell(new Barista.PrepareCoffee(e.coffee, e.guest), sender());
					return SupervisorStrategy.restart();
				})
				.build().orElse(super.supervisorStrategy().decider()));
	}

	private boolean approveCoffee(ApproveCoffee approveCoffee)
	{
	int count=	guestbook.get(approveCoffee.guest);
	if(count < caffineLimit)
	{   
		guestbook.put(approveCoffee.guest,++ count);
		log().info("Guest {} caffine count incremented",approveCoffee.guest);
		return true;
		}
	return false;
	
	}
	private void addGuestToGuestBook(ActorRef guest) {
		guestbook.put(guest,0);
		log().info("Guest {} add to book",guest);
		
	}
	private void removeGuestToGuestBook(ActorRef guest) {
		guestbook.remove(guest);
		log().debug("Removed Guest {}  from bookkeeper",guest);

	}

	protected ActorRef createBarista() {
		
		return getContext().actorOf(FromConfig.getInstance().props(
				Barista.props(prepareCoffeeDuration, baristaAccuracy)),"barista");
	}


	protected ActorRef createWaiter() {
		
	//	return getContext().actorOf(Waiter.props(barista),"waiter");
		return getContext().actorOf(Waiter.props(self(), barista, waiterMaxComplaintCount),"waiter");
	}


	protected ActorRef createGuest(final Coffee favoriteCoffee,final int guestCaffinelimit) {
		return context().actorOf(Guest.props(this.waiter ,favoriteCoffee, coffeeFinishedDuration,guestCaffinelimit));
	}
	public static Props props(int caffineLimit) {
		return Props.create(CoffeeHouse.class,()->new CoffeeHouse(caffineLimit));
	}
	
	/*
	 * public CoffeeHouse() { log().debug("CoffeHouse Open");
	 * 
	 * }
	 */
	
	
	public static final class CreateGuest{

		public final Coffee favoriteCoffee;
		public final int guestCaffineLimit;
		public  CreateGuest(Coffee favoriteCoffee,int guestCaffineLimit)
		{
			this.favoriteCoffee=favoriteCoffee;
			this.guestCaffineLimit=guestCaffineLimit;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			CreateGuest that = (CreateGuest) o;
			return guestCaffineLimit == that.guestCaffineLimit &&
					Objects.equals(favoriteCoffee, that.favoriteCoffee);
		}

		@Override
		public int hashCode() {
			return Objects.hash(favoriteCoffee, guestCaffineLimit);
		}

		@Override
		public String toString() {
			return "CreateGuest{" +
					"favoriteCoffee=" + favoriteCoffee +
					", guestCaffineLimit=" + guestCaffineLimit +
					'}';
		}
	}
    
	public static final class ApproveCoffee{
		
		public final ActorRef guest;
		public final Coffee coffee;
		public ApproveCoffee(Coffee coffee, ActorRef guest) {
			super();
			this.guest = guest;
			this.coffee = coffee;
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
			ApproveCoffee other = (ApproveCoffee) obj;
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
			return "ApproveCoffee [guest=" + guest + ", coffee=" + coffee + "]";
		}
		
		
		
	}


	public static final class GetStatus{
	    public static final GetStatus Instance=new GetStatus();
	    private GetStatus(){}
    }

    public static final class Status{
	    public final int guestCount;

        public Status(int guestCount) {
            this.guestCount = guestCount;
        }

        @Override
        public String toString() {
            return "Status{" +
                    "guestCount=" + guestCount +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Status status = (Status) o;
            return guestCount == status.guestCount;
        }

        @Override
        public int hashCode() {
            return Objects.hash(guestCount);
        }
    }
}
