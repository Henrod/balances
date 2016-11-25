# Balances

A Clojure library designed to simulate bank operations.

## Solution

The main variable is commonly called **ops**. It is a map where the keys are the account number and the value is a vector of Operations for that account.
Operation is a record with three attributes: description, amount and date.

1. description: A short string describing the operation.
2. amount: if the operation was credit, the amount must be positive; if it was debit, the amount must be negative; it's supposed that the sign is coherent with the description.
3. date: the date when the operation occurred, in the format dd/MM.

The solution has three layers, each one in its namespace:

* util: simple, common and pure functions; stateless namespace.
* core: main functions to operate over **ops**; the functions are also pure and the namespace is stateless.
* server: HTTP endpoints; this is the only layer with state which has the **ops** variable to hold all past operations of all accounts.

## Main functions

### new-operation
Creates a new Operation instance and update the map **ops**.
To update **ops**, the instance is conjoined with the vector that is the value for the account number key.

### current-balance
All operations of an account can be retrieved from **ops** by passing the account number as key.
Having these operations, the current balance is calculated by summing all signed amounts from all operations since the beginning.
 
### bank-statement
This function gets the log of operations of an account between two dates, namely **start date** and **end date**.
First, the balance is calculated for all dates at that account.
Then, the Operations are filtered to get those between **start date** and **end date**.
Finally, at each of these dates a new key-value is associated in the map: 

* key: date as string.
* value: map with the operations' descriptions and the balance.

### debt-periods
A vector is constructed. Each element of the vector is a map. The map contains a **start date**, a **principal** and maybe an **end date**.

First, the balance is calculated for all dates at the specified account.

This sequence is sorted by date.

Each new date that has a negative balance is added to the vector above.

If the balance becomes positive in a following date, the last element of the vector is updated with an **end date**.

If the balance changes but continues negative, then the last element is updated with an **end date** and a new element is added with a **start date** and a **principal**.

## Hypothesis

* The user that adds new operations is authenticated and using a secure connection (HTTPS).
* Following the exercise, it is not considered the year of the operations, but it's a simple extension that could be done in ns **util**.
* The user already has a valid account number over which he/she operates.
* The amount's sign and the description are coherent; for example, if the operation is a credit, the amount is positive, if it is a purchase, the amount is negative. This could be guaranteed by using a new parameter or checking from the description.

## Decisions

* The account number can be of any type: integer, UUID, string, etc. The tests use integer for the sake of readability.
* All HTTP requests are POST so they can accept JSON payloads as requests. 
* It is used double for the amounts instead of float for better truncations.
* It is used record instead of maps for better performance to access values and to standardize its attributes.

## Usage

To run as localhost at port 3000, execute:

```shell
lein run
```

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