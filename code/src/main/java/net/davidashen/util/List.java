/* $Id: List.java,v 1.2 2003/08/17 21:55:24 dvd Exp $ */

package net.davidashen.util;

/** Lispish list. 
Not very lispish, but still...
The compromise is that some of operations are
destructive.
*/

public class List implements Cloneable {
  private Link head, tail;
  private int length;

  final static class Link {
    Object data; Link next;
  }

 /** returns a mark set after the last element in the list */
  public final class Mark {
    Link link;
    int length;
    Mark() {
      link=tail.next;
      this.length=List.this.length;
    }
  }

 /** creates an empty list */
  public List() {
    clear();
  }

 /** empties the list */
  public final List clear() {
    head=new Link(); head.next=null; 
    tail=new Link(); tail.next=head; 
    length=0;
    return this;
  }

 /** calls a newInstance() to create a new list of the same type and then empties it
 @return the new list
 */
  protected List newList() {
    try {
      return ((List)super.clone()).clear();
    } catch(CloneNotSupportedException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

 /* clone/enloc are written to be as fast as possible */
 /** makes a shallow copy of the list */
  public Object clone() {
    List l=newList();
    if(isPair()) {
      Link link=head.next;
      for(;;) {
	l.snoc(link.data);
	if(link==tail.next) break;
	link=link.next;
      }
    }
    return l;
  }

 /** shallow copy reversed */
  public final List enolc() {
    List l=newList();
    if(isPair()) {
      Link link=head.next;
      for(;;) {
	l.cons(link.data);
	if(link==tail.next) break;
	link=link.next;
      }
    }
    return l;
  }

 /** reversed list */
  public final List reverse() {
    return length>1?enolc():this;
  }

 /** the same as cons */
  public final List unshift(Object o) {return cons(o);}
 /** prepend one element to the list, destructive.
 @param o the element to add
 @return this, modified by addition
 */
  public final List cons(Object o) {
    Link link=new Link();
    head.data=o; link.next=head; head=link;
    ++length;
    return this;
  }

 /** prepend another list, destructive.
 @param l the other list
 @return this, modified by addition
 */
  public final List prepend(List l) {
    if(l.isPair()) {
      l.tail.next.next=head.next; head.next=l.head.next; 
      length+=l.length;
    }
    return this;
  }

 /** the same as snoc */
  public final List append(Object o) {return snoc(o);}
 /** append one element to the list, destructive.
 @param o the object to add
 @return this, modified by addition
 */
  public final List snoc(Object o) {
    Link link=new Link();
    link.data=o; link.next=tail.next.next;
    tail.next.next=link; tail.next=link;
    ++length;
    return this;
  }

 /** append another list, destructive.
 @param l the other list
 @return this, modified by addition
 */
  public final List append(List l) {
    if(l.isPair()) {
      tail.next.next=l.head.next; tail.next=l.tail.next; length+=l.length;
    }
    return this;
  }

 /** remove first element and return it
 @return an object kept in the first link of the list
 */
  public final Object shift() {
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    head=head.next;
    --length;
    return head.data;
  }

 /** whether the list is empty */
  public final boolean isEmpty() {return !isPair();}
 /** whether the list is not empty (is a pair) */
  public final boolean isPair() {return head.next!=tail.next.next;}
 /** number of links in the list */
  public final int length() {return length;}

 /** the first element of the list
 @return the object kept in the first element
 */
  public final Object car() {
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    return head.next.data;
  }

 /** the last element of the list
 @return  the object kept in the last element
 */
  public final Object last() {
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    return tail.next.data;
  }

 /** the list's longest 'tail'
 @return an object of the same type holding  all the elements but the first one
 */
  public final List cdr() {
    List l=newList();
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    l.head.next=head.next.next; l.tail=tail; l.length=length-1;
    return l;
  }


 /** the list's shortest 'tail' 
 @return a list containing only the last element 
 */
  public final List cDr() {
    List l=newList();
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    l.head.next=tail.next; l.tail=tail; l.length=length-1;
    return l;
  }

 /** puts an object into the first element of the list */
  public final List setCar(Object o) {
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    head.next.data=o;
    return this;
  }

 /** binds value to the last link in the list */
  public final List setLast(Object o) {
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    tail.next.data=o;
    return this;
  }

 /** replaces the tail of a list with another list */
  public final List setCdr(List l) {
    if(!isPair()) throw new java.util.NoSuchElementException("list is not a pair");
    head.next.next=l.head.next;
    length=l.length+1;
    return this;
  }

 /** perform an operation on each element of the list 
 @param a applicator
 @see net.davidashen.util.Applicator
 */
  public final void foreach(Applicator a) {
    for(java.util.Enumeration e=elements();e.hasMoreElements();) a.f(e.nextElement());
  }

 /** (map (lambda (x) (...)) '(....))
 @param t list to put the new values into
 @param a applicator
 @return the list passed as t filled with new elements
 @see net.davidashen.util.Applicator
 */
  public final List map(List t /* target, to */, Applicator a) {
    for(java.util.Enumeration e=elements();e.hasMoreElements();) t.append(a.f(e.nextElement()));
    return t;
  }

 /** put a mark at the last element */
  public final Mark mark() {
    return new Mark();
  }

 /** store an object after the mark */
  public final List insert(Mark mark,Object o){
    Link link=new Link();
    link.next=mark.link.next; link.data=o; mark.link.next=link;
    if(tail.next==mark.link) tail.next=link;
    ++length;
    return this;
  }

 /** cut the list after the mark, the length value will be incorrect
  if elements are inserted before the mark */
  public final List cut(Mark mark) {
    tail.next=mark.link;
    length=mark.length;
    return this;
  }

 /** hygienic cut. The length is recomputed. */
  public final List hcut(Mark mark) {
    tail.next=mark.link;
    length=0;
    Link cur=head.next; while(cur!=tail.next.next) {++length; cur=cur.next;}
    return this;
  }

 /** insert a list after the mark */
  public final List insert(Link mark,List l) {
    Link next=mark.next; l.tail.next.next=next; mark.next=l.head.next;
    if(tail.next==mark) tail.next=l.tail.next;
    length+=l.length;
    return this;
  }

  public String toString() {return isPair()?cdr().addToString(car().toString()):"()";}
  private String addToString(String s) {
    if(isPair()) {
      return cdr().addToString(s+" "+car());
    } else {
      return "("+s+")";
    }
  }

  public final java.util.Enumeration elements() {return new Enumerator();}

  private class Enumerator implements java.util.Enumeration {
    private Link cur;
    Enumerator() {
      cur=head.next;
    }
    public final Object nextElement() {
      Object o;
      if(cur==tail.next.next) throw new java.util.NoSuchElementException("attempt to access element past the end of a list");
      o=cur.data;
      cur=cur.next;
      return o;
    }
    public final boolean hasMoreElements() {
      return cur!=tail.next.next;
    }
  }
}

/* $Log: List.java,v $
/* Revision 1.2  2003/08/17 21:55:24  dvd
/* Hyphenator.java is a java program
/*
* Revision 1.1  2003/08/17 20:31:00  dvd
* CVS keywords added
*/
