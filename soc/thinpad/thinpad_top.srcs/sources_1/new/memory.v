module memory(
    input wire clk,
    input wire rst,
    input wire[19:0] addr_in,
    input wire[31:0] data_in,
    input wire mem_read,
    input wire mem_write,
    input wire mem_ce_n,
    
    output wire[31:0]    data_out,
   
    inout wire[31:0]     sram_data,
    output wire[19:0]    sram_addr,
    output wire          sram_ce_n,
    output wire          sram_oe_n,
    output wire          sram_we_n,
    output wire[3:0]     sram_be_n,
   	input wire uart_dataready,
	input wire uart_tbre,
	input wire uart_tsre,
	output wire uart_rdn,
	output wire uart_wrn,
    output wire ram_ready


);

    reg reg_ram_ce,reg_ram_oe,reg_ram_we = 'b1;
    reg reg_uart_wrn,reg_uart_rdn = 'b1;
    reg[3:0] reg_ram_be = 'b1111;
    reg[31:0] reg_data = 32'b0;
    reg[19:0] reg_addr = 20'b0;
    reg reg_ready = 'b1;

    assign sram_be_n = reg_ram_be;
    assign sram_ce_n = reg_ram_ce;
    assign sram_we_n = reg_ram_we;
    assign sram_oe_n = reg_ram_oe;
    assign uart_rdn = reg_uart_rdn;
    assign uart_wrn = reg_uart_wrn;
    assign data_out = (!mem_ce_n & mem_read) ? sram_data : 32'bz;
    assign sram_data = (!mem_ce_n & mem_write) ? reg_data : 32'bz;
    assign sram_addr = reg_addr;
    assign ram_ready = reg_ready;

    parameter uart_addr = 'hBF00;


    reg[2:0] state = 'b000;

always@(posedge clk or posedge rst)
begin
    if (rst) begin
        reg_ram_ce <= 'b1;
        reg_ram_oe <= 'b1;
        reg_ram_we <= 'b1;
        reg_uart_wrn <= 'b1;
        reg_uart_rdn <= 'b1;
        reg_ram_be <= 'b1111;
        reg_data <= 32'b0;
        reg_addr <= 20'b0;
       // reg_data_out <= 32'b0;
        reg_ready <= 'b1;
    end
    else begin
        case (state)
            'b000: begin
                if (!mem_ce_n) begin
                    reg_ready <= 'b0;
                    reg_ram_ce <= 'b0;
                    reg_ram_be <= 'b0000;
                    reg_addr <= addr_in;
                    if (mem_write) begin
                        reg_data <= data_in;
                    end
                    state <= 'b001;
                end
            end  
            'b001: begin
                if (addr_in == uart_addr) begin
                    reg_ram_ce <= 'b1;
                    reg_ram_be <= 'b1111;
                    if (mem_read) begin
                        reg_uart_rdn <= 'b0;
                    end
                    else if (mem_write) begin
                        reg_uart_wrn <= 'b0;
                    end
                end
                else begin        
                    if (mem_read) begin
                        reg_ram_oe <= 'b0;
                    end
                    else if (mem_write) begin
                        reg_ram_we <= 'b0;
                    end
                end
                state <= 'b010;
            end
            'b010: begin
                reg_ram_ce <= 'b1;
                reg_ram_oe <= 'b1;
                reg_ram_we <= 'b1;
                reg_uart_wrn <= 'b1;
                reg_uart_rdn <= 'b1;
                reg_ram_be <= 'b1111;
                reg_ready <= 'b1;
                reg_data <= 32'b0;
                reg_addr <= 20'b0;
                state <= 'b000;
            end
            default: 
                state <= 'b000;
        endcase
    end
end

endmodule