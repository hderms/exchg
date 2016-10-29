package com.exchg
import akka.actor._
import akka.persistence._
import scala.collection.mutable.PriorityQueue

abstract trait BaseCmd
case class Cmd(data: Order) extends BaseCmd
case class EmptyCmd() extends BaseCmd
case class ExternalCmd(data: String) extends BaseCmd
case class Evt(data: Order)

case class State(events: List[Order] = Nil) {
  def updated(evt: Evt): State = copy(evt.data :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}
class ExamplePersistentActor extends PersistentActor {
  override def persistenceId = "exchg-inflow"

  val exchange = new TradeOffice()
  var state = State()

  def updateState(event: Evt): Unit =
    state = state.updated(event)

  def numEvents =
    state.size

  val receiveRecover: Receive = {
    case evt: Evt => updateState(evt)
    case SnapshotOffer(_, snapshot: State) => state = snapshot
  }

  val receiveCommand: Receive = {
    case Cmd(data) =>
      persist(Evt(data)) { event =>
        updateState(event)
        context.system.eventStream.publish(event)
        exchange.insert(data)
      }
    case "snap" => saveSnapshot(state)
    case "print" => {
      println("state is:")
      println(state)
      println("buyers:")
      println(exchange.buyers.orders)
      println("sellers:")
      println(exchange.sellers.orders)
    }
  }

}
sealed abstract trait Order {
  val price: Int
  val quantity: Int
}
case class BuyOrder(price: Int, quantity: Int) extends Order
case class SellOrder(price: Int, quantity: Int) extends Order

trait OrderOps[OrderType] {
  def matches(buy: Order, sell: Order): Boolean
}
object BuyOps extends OrderOps[BuyOrder] {

  def matches(buy: Order, sell: Order): Boolean = {
    buy.price >= sell.price

  }
}

object SellOps extends OrderOps[SellOrder] {

  def matches(buy: Order, sell: Order): Boolean = {
    buy.price <= sell.price

  }
}
trait OrderHandler[OrderType <: Order] {
  val orders: PriorityQueue[OrderType]
  val orderOps: OrderOps[OrderType]

  def insert(order: OrderType): Boolean = {
    orders += order

    return true
  }

  // Returns a list of completed orders and the remaining order if there is any
  def recurse(order: Order): (List[Order], Option[Order]) = {
    var matchedOrders: List[Order] = List()
    var ord: Order = order
    if (ord.quantity == 0) {
      return (matchedOrders, None)
    }
    while (!orders.isEmpty && ord.quantity > 0) {
      var candidateOrder: OrderType = orders.dequeue

      if (orderOps.matches(candidateOrder, ord)) {
        if (ord.quantity >= candidateOrder.quantity) {
          matchedOrders = matchedOrders :+ candidateOrder
          ord = ord match {
            case o: BuyOrder =>
              o.copy(quantity = o.quantity - candidateOrder.quantity)
            case o: SellOrder =>
              o.copy(quantity = o.quantity - candidateOrder.quantity)
          }
        } else {
          return (matchedOrders, Some(ord))
        }

      } else {
        if (candidateOrder.quantity > 0) {
          orders += candidateOrder
        }
        return (matchedOrders, Some(ord))
      }

    }
    return (matchedOrders, Some(ord))

  }
}
class BuyOrders extends OrderHandler[BuyOrder] {
  override val orderOps: OrderOps[BuyOrder] = BuyOps
  implicit object MaxOrder extends Ordering[BuyOrder] {
    def compare(x: BuyOrder, y: BuyOrder) = x.price compare y.price
  }
  val orders = new PriorityQueue[BuyOrder] //Gives MaxHeap
}

class SellOrders extends OrderHandler[SellOrder] {
  override val orderOps: OrderOps[SellOrder] = SellOps
  implicit object MinOrder extends Ordering[SellOrder] {
    def compare(x: SellOrder, y: SellOrder) = y.price compare x.price
  }

  val orders = new PriorityQueue[SellOrder]
}
sealed abstract trait OrderResult
case class CompletedOrder(constitutentOrders: List[Order]) extends OrderResult
case class IncompleteOrder(originalOrder: Order) extends OrderResult
class TradeOffice {
  val buyers = new BuyOrders()
  val sellers = new SellOrders()
  def findMatcher[O <: Order](order: O): (List[Order], Option[Order]) = {
    order match {
      case o: BuyOrder =>
        sellers.recurse(o)
      case o: SellOrder =>
        buyers.recurse(o)
    }
  }

  def insert(order: Order): OrderResult = {
    val ourOrders = findMatcher(order)
    ourOrders match {
      case (completed: List[Order], Some(remaining)) =>
        remaining match {
          case o: BuyOrder =>
            buyers.insert(o)
          case o: SellOrder =>
            sellers.insert(o)
        }
        println(s"completed the following orders: $completed")
        CompletedOrder(completed)
    }
  }
}
object Parser {
  val buyRe = "buy (\\d{1,5}) (\\d{1,5})".r
  val sellRe = "sell (\\d{1,5}) (\\d{1,5})".r
  val printRe = "print".r
  val quitRe = "quit".r
  def parse(s: String): BaseCmd = {
    s match {

      case buyRe(price, quantity) =>
        Cmd(BuyOrder(price.toInt, quantity.toInt))

      case sellRe(price, quantity) =>
        Cmd(SellOrder(price.toInt, quantity.toInt))
      case printRe() =>
        ExternalCmd("print")
      case quitRe() =>
        ExternalCmd("quit")
      case _ =>
        println("Failed to parse")
        EmptyCmd()
    }
  }
}

object Main extends App {
  val system = ActorSystem("foo")
  val myActor = system.actorOf(Props[ExamplePersistentActor])
  var line: String = ""
  while (true) {
    line = readLine()
    Parser.parse(line) match {
      case Cmd(o: Order) =>
        myActor ! Cmd(o)
      case ExternalCmd(s: String) =>
        s match {
          case "print" => myActor ! "print"
          case "quit" => System.exit(0)
        }
      case EmptyCmd() =>
      //noop
    }
  }
}
