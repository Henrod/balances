# Balances

A Clojure library designed to simulate bank operations.

## Solution

The main variable is commonly called **ops**. It's a map where each key is an account number and the value is another map.
This map has two keys, :current which saves the current balance of this account number, and :operations which holds another map.
Now, this map is a sorted-map (balanced tree) where each key is the date when operations occurred; the value of each key is a vector of Operation. The map is sorted by date.
Operation is a record with two attributes: description and amount.
Example of **ops** (in the example below the dates are strings for the sake of clarity; instead, they are actual instances of org.joda.time.DateTime):

```
{1 {:current 771.43M,
       :operations {"15/10" [#balances.core.Operation
                                                 {:amount 1000M,
                                                   :description "Deposit"}],
                              "16/10" [#balances.core.Operation
                                                 {:amount -45.23M,
                                                   :description "Purchase on Uber"}
                                              #balances.core.Operation
                                                 {:amount -3.34M,
                                                   :description "Purchase on Amazon"}],
                              "17/10" [#balances.core.Operation
                                                 {:amount -180M,
                                                   :description "Withdrawal"}]}},
 2 {:current 71.43M,
      :operations {"17/10" [#balances.core.Operation
                                                {:amount 771.43M,
                                                  :description "Credit"}],
                              "18/10" [#balances.core.Operation
                                                 {:amount -800M,
                                                   :description "Purchase of flight ticket"}],
                              "25/10" [#balances.core.Operation
                                                {:amount 100M,
                                                  :description "Deposit"}]}}}
```

The following parameters describe each operation. 

1. account: unique identifier for an account; can be any number or string. 
2. description: a short string describing the operation.
3. amount: it is only allowed numbers different than zero and with zero, one or two decimal places. For example, it's not possible to send 10.123 as amount.
4. date: the date when the operation occurred, in the format dd/MM.

The solution has three layers, each one in its namespace:

* util: simple, common and pure functions; stateless namespace.
* core: main functions to operate over **ops**; the functions are also pure and the namespace is stateless. It also validates the inputs according to constraints.
* server: HTTP endpoints; this is the only layer with state which has the **ops** variable to hold all past operations of all accounts.

## Main functions

### new-operation
Creates a new Operation instance and update the map **ops**.
To update **ops**, the amount is added to :current and the map of :operations is updated.
To update :operations, the new Operation is conjoined at its corresponding date's vector.

### current-balance
All operations of an account can be retrieved from **ops** by passing the account number as key.
Since at each new operation the value of :current for the account is updated, the current balance can be retrieved in a direct look-up for :current.
 
### bank-statement
This function returns the log of operations of an account between two dates, namely **start date** and **end date**.

First, the balance is calculated for each dates at that account.
Then, the dates are filtered to get those between **start date** and **end date**.
Since the dates are hold in a tree inside **ops** (sorted-map), they are already ordered by date.
Finally, at each of these dates a new map is conjoined to the vector of :statement.
This map has the fields:

* date: date when operations happened.
* balance: balance of that date based on all previous history.
* periods: vector of descriptions of each operation at that date.

Since the application is to print the statement, the order of the operations is important. 
And since the dates were already sorted from the tree, each conjoin add the element in the vector sorted by date.

### debt-periods
A vector is constructed. Each element of the vector is a map. The map contains a **start date**, a **principal** and maybe an **end date**.

First, the balance is calculated for all dates at the specified account.

This sequence is already sorted by date since it is used a sorted-map (balanced tree).

Each new date that has a negative balance is added to the vector above.

If the balance becomes positive in a following date, the last element of the vector is updated with an **end date**.

If the balance changes but continues negative, then the last element is updated with an **end date** and a new element is added with a **start date** and a **principal**.

If two periods have the same negative balance and one starts the day the other one ends, this becomes a **plateau**. 
So, these two periods are merged with only one **start date** and **end date**.

Just as bank-statement, the order to show the periods is important. 
They are already conjoined in the vector ordered since the operations were already ordered inside the tree (sorted-map).

## Hypothesis

* The user that adds new operations is authenticated and using a secure connection (HTTPS).
* Following the exercise, it is not considered the year of the operations, but it's a simple extension that could be done in ns **util**.
* The user already has a valid account number over which he/she operates.
* The amount's sign and the description are coherent; for example, if the operation is a credit, the amount is positive, if it is a purchase, the amount is negative. This could be guaranteed by using a new parameter or checking from the description.
* There aren't problems with TCP retransmission of a transaction from client to server (due to connection failure, for example).

## Decisions

* The account number can be of any type: integer, UUID, string, etc. The tests use integer for the sake of readability.
* All HTTP requests are POST so they can accept JSON payloads as requests. 
* Amounts are always BigDecimal. The value showed on JSONs are strings taken two decimal places only.
* It is used record instead of maps for better performance to access values and to standardize its attributes.

## Time Complexities

#### new-operation: O(lg(m))
Where m is the number of dates where operations happened in this account, and lg(m) is the log(m) on base 2.

The time to update :current is O(1).

To find the correct date in the balanced tree or to insert a new date in it, the time is O(lg(m)).

And the time to conjoin a vector is O(1).

So the total time is bounded by O(lg(m)).

#### current-balance: O(1)
The current balance is updated at each new-operation, and to access it a simple look-up for :current is done in the account's map.

#### bank-statement: O(n)
Where n is the number of operations the given account has ever done. 

Access vector of operations is O(1) in the map. 

There is a first pass to calculate the total of amount operated at each date. Since all operations are accessed, the time complexity is O(n).

In the second pass, the amount from operations in a date is added to the balance of the previous date, so the balance in the current date is calculated based on all previous operations. This calculation is done in O(m).

Then there is a third pass to filter dates outside **start-date** and **end-date**, which is O(m).

With these results, a vector is constructed by conjoining maps with values of the operations between **start-date** and **end-date**. 

So the time complexity is O(m + n), where n is the number of operations and m is the number of dates where operations happened. Since m < n, then the final complexity is O(n).

Notice that since the dates are already sorted in the sorted-map, it's saved the time to sort them, which would be O(m*lg(m)).

#### debt-periods: O(n)
In the same way above, computation of the balance at each date is done in O(n).

Then, a second pass in the dates is done to look for negative balances to add in debt-periods, which is O(m).

Since n > m, the time is bounded by O(n).

A tweak could be done to transform O(n) to O(m) by saving in the map the total amount operated per date.
But since n isn't much greater than m, it would not make significant difference. 

## Usage

To run as localhost at port 3000, execute:

```shell
lein run
```

To call each function, send an HTTP POST with the following parameters: 

| function        | address    | parameters                         |
|-----------------|------------|------------------------------------|
| new-operation   | /new       | account, amount, description, date |
| current-balance | /balance   | account                            |
| bank-statement  | /statement | account, start, end                |
| debt-periods    | /debt      | account                            |

To test all namespaces, execute:
```shell
lein test
```

To test a specific namespace add its name after, for example:
```shell
lein test balances.core-test
```

## License

Copyright © 2016 Henrique Rodrigues

Distributed under the MIT License.