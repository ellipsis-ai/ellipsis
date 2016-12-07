#!/bin/sh
exec scala "$0" "$@"
!#

import jp.co.bizreach.dynamodb4s._
import awscala.dynamodbv2.DynamoDB

// for local environment
implicit val db = DynamoDB.local()

// for AWS environment
implicit val db = DynamoDB.apply(accessKeyId = "xxx", secretAccessKey = "xxx")

object Hello extends App {
    println("Hello, world")
    // if you want to access the command line args:
    //args.foreach(println)
}

Hello.main(args)
