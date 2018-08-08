module memory(
    input wire clk,
    input wire rst,
    input wire[31:0] addr_in,
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
	
    output wire ram_ready,

    output wire	flash_finished,
    output wire[22:0] flash_addr,
    inout wire[15:0] flash_data,
    output wire flash_byte,
    output wire flash_vpen,
    output wire flash_rp,
    output wire flash_ce,
    output wire flash_oe,
    output wire flash_we

);

    reg reg_ram_ce,reg_ram_oe,reg_ram_we = 'b1;
    reg reg_uart_wrn,reg_uart_rdn = 'b1;
    reg[3:0] reg_ram_be = 'b1111;
    reg[31:0] reg_data = 32'b0;
    reg[31:0] reg_data_out = 32'b0;
    reg[19:0] reg_addr = 20'b0;
    reg reg_ready = 'b0;
    reg read_ok = 'b0;

    reg rflag = 'b1;
    //reg flash_finished_tmp = 'b1;
    reg flash_finished_tmp = 'b0;
    reg[2:0] flash_state = 'b000;
    reg[15:0] current_addr = 16'b0;
    reg[15:0] ram_load_addr = 16'b0;
    reg[22:0] reg_flash_addr = 23'b0;
    reg[15:0] reg_flash_data = 16'b0;
    integer cnt = 0;
    integer write_wait_count = 0;

    reg reg_flash_we,reg_flash_oe,reg_flash_byte,reg_flash_vpen,reg_flash_rp,reg_flash_ce = 'b1;

    reg[15:0] flash_end_addr = 'h1000;	

    parameter uart_addr = 'h20000000;
    parameter check_addr = 'h20000001;

    assign sram_be_n = reg_ram_be;
    assign sram_ce_n = reg_ram_ce;
    assign sram_we_n = reg_ram_we;
    assign sram_oe_n = reg_ram_oe;
    assign uart_rdn = reg_uart_rdn;
    assign uart_wrn = reg_uart_wrn;
    assign data_out = reg_data_out;
	assign sram_data = (((!sram_ce_n) && (!sram_we_n)) || ((!reg_uart_wrn) && sram_ce_n)) ? reg_data : 32'bz;
    assign sram_addr = reg_addr;
    assign ram_ready = reg_ready;

    assign flash_finished = flash_finished_tmp;
    assign flash_we = reg_flash_we; 
	assign flash_oe = reg_flash_oe;
    assign flash_addr = reg_flash_addr;
    assign flash_data = ((!flash_ce) && (!flash_we)) ? reg_flash_data : 16'bz;
							
	assign flash_byte = reg_flash_byte; 
	assign flash_vpen = reg_flash_vpen; 
	assign flash_rp = reg_flash_rp;
	assign flash_ce = reg_flash_ce; 
    


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
        state <= 'b000;
       // reg_data_out <= 32'b0;
        //reg_ready <= 'b1;

        flash_state <= 'b000;   
		current_addr <= 16'b0;
		reg_flash_addr <= 22'b0;
		ram_load_addr <= 16'b0; 
		//flash_finished_tmp <= 'b1;
        reg_flash_data <= 16'b0;
        reg_flash_byte <= 'b1;
		reg_flash_vpen <= 'b1;
		reg_flash_rp <= 'b1;
		reg_flash_ce <= 'b1;
        reg_flash_oe <= 'b1;
        reg_flash_we <= 'b1;
		flash_finished_tmp <= 'b0;
        reg_ready <= 'b0;
    end
    else begin
        if (flash_finished == 'b1) begin
            reg_flash_ce <= 'b1;
            case (state)
                'b000: begin
                    if (!mem_ce_n) begin
                        reg_ready <= 'b0;
                        state <= 'b001;
                    end 
                end
                'b001: begin
                    reg_ram_ce <= 'b0;
                    reg_ram_be <= 'b0000;
                    reg_data_out <= 32'b0;
                    reg_addr <= addr_in[19:0];
                    if (mem_write) begin
                        reg_data <= data_in;
                    end
                    state <= 'b010;
                    
                end  
                'b010: begin
                    if (addr_in == uart_addr) begin
                        reg_ram_ce <= 'b1;
                        reg_ram_be <= 'b0000;
                        if (mem_read) begin
                            reg_uart_rdn <= 'b0;
                        end
                        else if (mem_write) begin
                            reg_data[31:8] <= 24'b0;
                            reg_uart_wrn <= 'b0;
                        end
                    end
                    else if (addr_in == check_addr) begin
                        reg_data_out[0] <= uart_tsre & uart_tbre;
                        reg_data_out[4] <= uart_dataready;
                    end
                    else begin        
                        if (mem_read) begin
                            reg_ram_oe <= 'b0;
                        end
                        else if (mem_write) begin
                            reg_ram_we <= 'b0;
                        end
                    end
                    state <= 'b011;
                end
                'b011: begin
                    reg_ram_ce <= 'b1;
                    reg_ram_oe <= 'b1;
                    reg_ram_we <= 'b1;
                    reg_uart_wrn <= 'b1;
                    reg_uart_rdn <= 'b1;
                    reg_ram_be <= 'b1111;
                    reg_ready <= 'b1;
                    reg_data <= 32'b0;
                    reg_addr <= 20'b0;
                    if (mem_read && (addr_in != check_addr)) begin
                        reg_data_out <= sram_data;
                    end
                    state <= 'b000;
                end
                default: 
                    state <= 'b000;
            endcase
        end
        else begin
            reg_flash_ce <= 'b0;
            reg_ready <= 'b0;
			if (cnt >= 1000) begin
				cnt <= 0;
				case (flash_state)		
				    'b000: begin
						reg_ram_ce <= 'b0;
                        reg_ram_be <= 'b0000;
						reg_ram_we <= 'b1;
						reg_ram_oe <= 'b1;
						reg_uart_wrn <= 'b1;
						reg_uart_rdn <= 'b1;
						//reg_flash_we <= 'b0;
                        reg_flash_oe <= 'b1;
							
						reg_flash_byte <= 'b1;
						reg_flash_vpen <= 'b1;
						reg_flash_rp <= 'b1;
						reg_flash_ce <= 'b0;
						flash_state <= 'b001;
                    end
					
                    'b001: begin
                        reg_flash_data <= 'h00FF;
                        reg_flash_we <= 'b0;
						flash_state <= 'b010;
                    end
							
                    'b010: begin
                        reg_flash_we <= 'b1;
                        reg_flash_addr <= 22'b0;
						flash_state <= 'b011;
                    end

                    'b011: begin
                        reg_flash_oe <= 'b0;
						reg_flash_addr[16:1] <= current_addr;
						flash_state <= 'b100;
                    end

                    'b100: begin
                        reg_addr[19:16] <= 4'b0;
                        reg_addr[15:0] <= ram_load_addr;
                        if (read_ok) begin
                            reg_data[31:16] <= flash_data;
						    reg_ram_we <= 'b0;
						    read_ok <= 'b0;
						    flash_state <= 'b101;
                        end
                        else begin
                            reg_data[15:0] <= flash_data;
                            current_addr <= current_addr + 1;
                            read_ok <= 'b1;
                            flash_state <= 'b000;
                        end
                        reg_flash_oe <= 'b1;
                    end
					
                    'b101: begin
                        reg_ram_we <= 'b1;
						current_addr <= current_addr + 1;
						ram_load_addr <= ram_load_addr + 1;
						flash_state <= 'b000;
                    end
					
                    default: begin
                        flash_state <= 'b000;
                    end
				endcase
					
				if (current_addr >= flash_end_addr) begin
						current_addr <= 16'b0;
						ram_load_addr <= 16'b0;
						flash_finished_tmp <= 'b1;
                        reg_ram_ce <= 'b1;
                        //reg_ready <= 'b1;
                end
            end

			else begin
				cnt <= cnt + 1;
			end
        end
    end
end

endmodule
