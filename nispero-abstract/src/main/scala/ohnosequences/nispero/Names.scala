package ohnosequences.nispero

import ohnosequences.awstools.dynamodb._
import ohnosequences.awstools.dynamodb.HashKey
import ohnosequences.awstools.dynamodb.RangeKey
import ohnosequences.awstools.dynamodb.NumericValue
import ohnosequences.awstools.ec2.Tag

object Names {

  val PRODUCT_PREFIX = "nispero"


  object Tables {
    val WORKERS_STATE_TABLE_PREFIX = PRODUCT_PREFIX + "WorkersState"
    val UNIQUE_ID_TABLE = PRODUCT_PREFIX + "UniqueId"

    val UNIQUE_HASH_KEY = HashKey("id", StringType)
    val UNIQUE_RANGE_KEY = RangeKey("range", NumericType)

    val WORKERS_STATE_HASH_KEY = HashKey("id", NumericType)
    val WORKERS_STATE_HASH_KEY_VALUE = NumericValue(1)
    val WORKERS_STATE_RANGE_KEY = RangeKey("timestamp", NumericType)
  }

}


