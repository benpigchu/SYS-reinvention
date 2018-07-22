package reinventation_core

import chisel3._
import chisel3.util._

object ImmGen {
	def apply(sel:UInt,inst:UInt)={
		import ImmTypeSignal._
		val sign=inst(31)
		val i30_20=Mux(sel===IMM_U,inst(30,20),Fill(11,sign))
		val i19_12=Mux((sel===IMM_U)||(sel===IMM_J),inst(19,12),Fill(8,sign))
		val i11=Mux(sel===IMM_B,inst(7),Mux(sel===IMM_U,0.U(1.W),Mux(sel===IMM_J,inst(20),sign)))
		val i10_5=Mux(sel===IMM_U,0.U(6.W),inst(30,25))
		val i4_1=Mux(sel===IMM_U,0.U(4.W),Mux((sel===IMM_S)||(sel===IMM_B),inst(11,8),inst(24,21)))
		val i0=Mux(sel===IMM_I,inst(20),Mux(sel===IMM_S,inst(7),0))
		Cat(sign,i30_20,i19_12,i11,i10_5,i4_1,i0)
	}
}